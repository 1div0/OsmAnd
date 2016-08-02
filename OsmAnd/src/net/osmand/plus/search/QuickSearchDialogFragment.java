package net.osmand.plus.search;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreAPI;
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchWord;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class QuickSearchDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "QuickSearchDialogFragment";
	private static final String QUICK_SEARCH_QUERY_KEY = "quick_search_query_key";
	private static final String QUICK_SEARCH_SEARCHING_KEY = "quick_search_searching_key";
	private Toolbar toolbar;
	private LockableViewPager viewPager;
	private SearchFragmentPagerAdapter pagerAdapter;
	private TabLayout tabLayout;
	private View tabToolbarView;
	private View tabsView;
	private View searchView;
	private View buttonToolbarView;
	private ImageView buttonToolbarImage;
	private TextView buttonToolbarText;
	private QuickSearchMainListFragment mainSearchFragment;
	private QuickSearchHistoryListFragment historySearchFragment;
	private QuickSearchCategoriesListFragment categoriesSearchFragment;

	private Toolbar toolbarEdit;
	private TextView titleEdit;

	private EditText searchEditText;
	private ProgressBar progressBar;
	private ImageButton clearButton;

	private OsmandApplication app;
	private QuickSearchHelper searchHelper;
	private SearchUICore searchUICore;
	private String searchQuery = "";

	private net.osmand.Location location = null;
	private Float heading = null;
	private boolean useMapCenter;
	private boolean paused;
	private boolean searching;
	private boolean foundPartialLocation;

	private boolean newSearch;
	private boolean interruptedSearch;

	private static final double DISTANCE_THRESHOLD = 70000; // 70km


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		boolean isLightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	@SuppressLint("PrivateResource")
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final MapActivity mapActivity = getMapActivity();
		final View view = inflater.inflate(R.layout.search_dialog_fragment, container, false);

		if (savedInstanceState != null) {
			searchQuery = savedInstanceState.getString(QUICK_SEARCH_QUERY_KEY);
			interruptedSearch = savedInstanceState.getBoolean(QUICK_SEARCH_SEARCHING_KEY, false);
		}
		if (searchQuery == null) {
			searchQuery = getArguments().getString(QUICK_SEARCH_QUERY_KEY);
			newSearch = true;
		}
		if (searchQuery == null)
			searchQuery = "";

		tabToolbarView = view.findViewById(R.id.tab_toolbar_layout);
		tabsView = view.findViewById(R.id.tabs_view);
		searchView = view.findViewById(R.id.search_view);

		buttonToolbarView = view.findViewById(R.id.button_toolbar_layout);
		buttonToolbarImage = (ImageView) view.findViewById(R.id.buttonToolbarImage);
		buttonToolbarImage.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_marker_dark));
		buttonToolbarText = (TextView) view.findViewById(R.id.buttonToolbarTitle);
		view.findViewById(R.id.buttonToolbar).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SearchPhrase searchPhrase = searchUICore.getPhrase();
				if (foundPartialLocation) {
					QuickSearchCoordinatesFragment.showDialog(QuickSearchDialogFragment.this, searchPhrase.getUnknownSearchWord());
				} else if (searchPhrase.isNoSelectedType() || searchPhrase.isLastWord(ObjectType.POI_TYPE)) {
					PoiUIFilter filter;
					if (searchPhrase.isNoSelectedType()) {
						filter = new PoiUIFilter(null, app, "");
					} else {
						AbstractPoiType abstractPoiType = (AbstractPoiType) searchPhrase.getLastSelectedWord().getResult().object;
						filter = new PoiUIFilter(abstractPoiType, app, "");
					}
					if (!Algorithms.isEmpty(searchPhrase.getUnknownSearchWord())) {
						filter.setFilterByName(searchPhrase.getUnknownSearchWord());
					}
					app.getPoiFilters().clearSelectedPoiFilters();
					app.getPoiFilters().addSelectedPoiFilter(filter);
					getMapActivity().setQuickSearchTopbarActive(true);
					getMapActivity().refreshMap();
					hide();
				} else {
					SearchWord word = searchPhrase.getLastSelectedWord();
					if (word != null && word.getLocation() != null) {
						SearchResult searchResult = word.getResult();
						String name = QuickSearchListItem.getName(app, searchResult);
						String typeName = QuickSearchListItem.getTypeName(app, searchResult);
						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, typeName, name);
						app.getSettings().setMapLocationToShow(
								searchResult.location.getLatitude(), searchResult.location.getLongitude(),
								searchResult.preferredZoom, pointDescription, true, searchResult.object);

						getMapActivity().setQuickSearchTopbarActive(true);
						MapActivity.launchMapActivityMoveToTop(getActivity());
						hide();
					}
				}
			}
		});

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getIconsCache().getThemedIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		toolbarEdit = (Toolbar) view.findViewById(R.id.toolbar_edit);
		toolbarEdit.setNavigationIcon(app.getIconsCache().getIcon(R.drawable.ic_action_remove_dark));
		toolbarEdit.setNavigationContentDescription(R.string.shared_string_cancel);
		toolbarEdit.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enableSelectionMode(false, -1);
			}
		});

		titleEdit = (TextView) view.findViewById(R.id.titleEdit);
		view.findViewById(R.id.shareButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();
				List<QuickSearchListItem> selectedItems = historySearchFragment.getListAdapter().getSelectedItems();
				for (QuickSearchListItem searchListItem : selectedItems) {
					HistoryEntry historyEntry = (HistoryEntry) searchListItem.getSearchResult().object;
					historyEntries.add(historyEntry);
				}
				if (historyEntries.size() > 0) {
					shareHistory(historyEntries);
					enableSelectionMode(false, -1);
				}
			}
		});
		view.findViewById(R.id.deleteButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				new DialogFragment() {
					@NonNull
					@Override
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setTitle(R.string.confirmation_to_delete_history_items)
								.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
										List<QuickSearchListItem> selectedItems = historySearchFragment.getListAdapter().getSelectedItems();
										for (QuickSearchListItem searchListItem : selectedItems) {
											HistoryEntry historyEntry = (HistoryEntry) searchListItem.getSearchResult().object;
											helper.remove(historyEntry);
										}
										reloadHistory();
										enableSelectionMode(false, -1);
									}
								})
								.setNegativeButton(R.string.shared_string_no, null);
						return builder.create();
					}
				}.show(getChildFragmentManager(), "DeleteHistoryConfirmationFragment");
			}
		});

		viewPager = (LockableViewPager) view.findViewById(R.id.pager);
		pagerAdapter = new SearchFragmentPagerAdapter(getChildFragmentManager(), getResources());
		viewPager.setAdapter(pagerAdapter);

		tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
		tabLayout.setupWithViewPager(viewPager);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				hideKeyboard();
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});

		searchEditText = (EditText) view.findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				String newQueryText = s.toString();
				updateClearButtonAndHint();
				updateClearButtonVisibility(true);
				updateTabbarVisibility(newQueryText.length() == 0);
				if (!searchQuery.equalsIgnoreCase(newQueryText)) {
					searchQuery = newQueryText;
					if (Algorithms.isEmpty(searchQuery)) {
						searchUICore.resetPhrase();
					} else {
						runSearch();
					}
				}
			}
		});
		
		progressBar = (ProgressBar) view.findViewById(R.id.searchProgressBar);
		clearButton = (ImageButton) view.findViewById(R.id.clearButton);
		clearButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
		clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (searchEditText.getText().length() > 0) {
					String newText = searchUICore.getPhrase().getTextWithoutLastWord();
					searchEditText.setText(newText);
					searchEditText.setSelection(newText.length());
				} else if (useMapCenter && location != null) {
					useMapCenter = false;
					updateUseMapCenterUI();
					startLocationUpdate();
					LatLon centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
					SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
							new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
					searchUICore.updateSettings(ss);
					updateClearButtonAndHint();
					updateClearButtonVisibility(true);
				}
				updateToolbarButton();
			}
		});

		setupSearch(mapActivity);

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		updateToolbarButton();
		updateClearButtonAndHint();
		updateClearButtonVisibility(true);
		addMainSearchFragment();

		searchEditText.requestFocus();
		AndroidUtils.softKeyboardDelayed(searchEditText);
	}

	public String getText() {
		return searchEditText.getText().toString();
	}

	public void hideKeyboard() {
		if (searchEditText.hasFocus()) {
			AndroidUtils.hideSoftKeyboard(getActivity(), searchEditText);
		}
	}

	public void show() {
		getMapActivity().setQuickSearchTopbarActive(false);
		if (useMapCenter) {
			LatLon mapCenter = getMapActivity().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
			SearchSettings ss = searchUICore.getSearchSettings().setOriginalLocation(
					new LatLon(mapCenter.getLatitude(), mapCenter.getLongitude()));
			searchUICore.updateSettings(ss);
			updateLocationUI(mapCenter, null);
		}
		getDialog().show();
		paused = false;
	}

	public void hide() {
		paused = true;
		getDialog().hide();
	}

	public void closeSearch() {
		app.getPoiFilters().clearSelectedPoiFilters();
		dismiss();
	}

	public void addMainSearchFragment() {
		mainSearchFragment = (QuickSearchMainListFragment) Fragment.instantiate(this.getContext(), QuickSearchMainListFragment.class.getName());
		FragmentManager childFragMan = getChildFragmentManager();
		FragmentTransaction childFragTrans = childFragMan.beginTransaction();
		childFragTrans.replace(R.id.search_view, mainSearchFragment);
		childFragTrans.commit();
	}

	private void updateToolbarButton() {
		if (foundPartialLocation) {
			buttonToolbarText.setText(app.getString(R.string.advanced_coords_search).toUpperCase());
		} else if (searchEditText.getText().length() > 0) {
			SearchWord word = searchUICore.getPhrase().getLastSelectedWord();
			if (word != null && word.getResult() != null) {
				buttonToolbarText.setText(app.getString(R.string.show_something_on_map, word.getResult().localeName).toUpperCase());
			} else {
				buttonToolbarText.setText(app.getString(R.string.show_on_map).toUpperCase());
			}
		} else {
			buttonToolbarText.setText(app.getString(R.string.show_on_map).toUpperCase());
		}
	}

	private void setupSearch(final MapActivity mapActivity) {
		// Setup search core
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
		searchHelper = app.getSearchUICore();
		searchUICore = searchHelper.getCore();
		if (newSearch) {
			setResultCollection(null);
			searchUICore.resetPhrase();
		}

		location = app.getLocationProvider().getLastKnownLocation();
		LatLon clt = mapActivity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		LatLon centerLatLon = clt;
		searchEditText.setHint(R.string.search_poi_category_hint);
		if (location != null) {
			double d = MapUtils.getDistance(clt, location.getLatitude(), location.getLongitude());
			if (d < DISTANCE_THRESHOLD) {
				centerLatLon = new LatLon(location.getLatitude(), location.getLongitude());
			} else {
				useMapCenter = true;
			}
		}
		SearchSettings settings = searchUICore.getSearchSettings().setOriginalLocation(
				new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude()));
		settings = settings.setLang(locale);
		searchUICore.updateSettings(settings);
		searchUICore.setOnResultsComplete(new Runnable() {
			@Override
			public void run() {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						searching = false;
						if (!paused) {
							hideProgressBar();
							addMoreButton();
						}
					}
				});
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setShowsDialog(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
		outState.putBoolean(QUICK_SEARCH_SEARCHING_KEY, searching);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!useMapCenter) {
			startLocationUpdate();
		}
		paused = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		stopLocationUpdate();
		hideProgressBar();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			getMapActivity().setQuickSearchTopbarActive(false);
			getChildFragmentManager().popBackStack();
		}
		super.onDismiss(dialog);
	}

	public Toolbar getToolbar() {
		return toolbar;
	}

	public boolean isUseMapCenter() {
		return useMapCenter;
	}

	private void startLocationUpdate() {
		app.getLocationProvider().addCompassListener(this);
		app.getLocationProvider().addLocationListener(this);
		location = app.getLocationProvider().getLastKnownLocation();
		updateLocation(location);
	}

	private void stopLocationUpdate() {
		app.getLocationProvider().removeLocationListener(this);
		app.getLocationProvider().removeCompassListener(this);
	}

	private void showProgressBar() {
		updateClearButtonVisibility(false);
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		updateClearButtonVisibility(true);
		progressBar.setVisibility(View.GONE);
	}

	private void updateClearButtonAndHint() {
		if (useMapCenter && searchEditText.length() == 0) {
			LatLon latLon = searchUICore.getSearchSettings().getOriginalLocation();
			double d = MapUtils.getDistance(latLon, location.getLatitude(), location.getLongitude());
			String dist = OsmAndFormatter.getFormattedDistance((float) d, app);
			searchEditText.setHint(getString(R.string.dist_away_from_my_location, dist));
			clearButton.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_get_my_location, R.color.color_myloc_distance));
		} else {
			searchEditText.setHint(R.string.search_poi_category_hint);
			clearButton.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_remove_dark));
		}
	}

	private void updateClearButtonVisibility(boolean show) {
		if (show) {
			clearButton.setVisibility(searchEditText.length() > 0 || useMapCenter ? View.VISIBLE : View.GONE);
		} else {
			clearButton.setVisibility(View.GONE);
		}
	}

	private void updateTabbarVisibility(boolean show) {
		if (show && tabsView.getVisibility() == View.GONE) {
			tabToolbarView.setVisibility(View.VISIBLE);
			buttonToolbarView.setVisibility(View.GONE);
			tabsView.setVisibility(View.VISIBLE);
			searchView.setVisibility(View.GONE);
		} else if (!show && tabsView.getVisibility() == View.VISIBLE) {
			tabToolbarView.setVisibility(View.GONE);
			buttonToolbarView.setVisibility(View.VISIBLE);
			tabsView.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
		}
	}

	public void setResultCollection(SearchResultCollection resultCollection) {
		searchHelper.setResultCollection(resultCollection);
	}

	public SearchResultCollection getResultCollection() {
		return searchHelper.getResultCollection();
	}

	public void onSearchListFragmentResume(QuickSearchListFragment searchListFragment) {
		switch (searchListFragment.getType()) {
			case HISTORY:
				historySearchFragment = (QuickSearchHistoryListFragment) searchListFragment;
				reloadHistory();
				break;

			case CATEGORIES:
				categoriesSearchFragment = (QuickSearchCategoriesListFragment) searchListFragment;
				reloadCategories();
				break;

			case MAIN:
				if (!Algorithms.isEmpty(searchQuery)) {
					searchEditText.setText(searchQuery);
					searchEditText.setSelection(searchQuery.length());
				}
				if (getResultCollection() != null) {
					updateSearchResult(getResultCollection(), false);
					addMoreButton();
				}
				break;
		}
		LatLon mapCenter = getMapActivity().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		if (useMapCenter) {
			searchListFragment.updateLocation(mapCenter, null);
		}
	}

	public void reloadCategories() {
		SearchAmenityTypesAPI amenityTypesAPI =
				new SearchAmenityTypesAPI(app.getPoiTypes());
		final List<SearchResult> amenityTypes = new ArrayList<>();
		SearchPhrase sp = new SearchPhrase(null).generateNewPhrase("", searchUICore.getSearchSettings());
		try {
			amenityTypesAPI.search(sp, new SearchResultMatcher(
					new ResultMatcher<SearchResult>() {
						@Override
						public boolean publish(SearchResult object) {
							amenityTypes.add(object);
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					}, 0, new AtomicInteger(0), -1));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (amenityTypes.size() > 0) {
			searchUICore.sortSearchResults(sp, amenityTypes);
			List<QuickSearchListItem> rows = new ArrayList<>();
			for (SearchResult sr : amenityTypes) {
				rows.add(new QuickSearchListItem(app, sr));
			}
			categoriesSearchFragment.updateListAdapter(rows, false);
		}
	}

	public void reloadHistory() {
		SearchHistoryAPI historyAPI = new SearchHistoryAPI(app);
		final List<SearchResult> history = new ArrayList<>();
		SearchPhrase sp = new SearchPhrase(null).generateNewPhrase("", searchUICore.getSearchSettings());
		historyAPI.search(sp, new SearchResultMatcher(
				new ResultMatcher<SearchResult>() {
					@Override
					public boolean publish(SearchResult object) {
						history.add(object);
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}, 0, new AtomicInteger(0), -1));
		List<QuickSearchListItem> rows = new ArrayList<>();
		if (history.size() > 0) {
			for (SearchResult sr : history) {
				rows.add(new QuickSearchListItem(app, sr));
			}
		}
		historySearchFragment.updateListAdapter(rows, false);
	}

	private void runSearch() {
		runSearch(searchQuery);
	}

	private void runSearch(String text) {
		showProgressBar();
		SearchSettings settings = searchUICore.getPhrase().getSettings();
		if(settings.getRadiusLevel() != 1){
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runCoreSearch(text, true);
	}

	private void runCoreSearch(final String text, final boolean updateResult) {
		showProgressBar();
		foundPartialLocation = false;
		updateToolbarButton();

		if (app.isApplicationInitializing() && text.length() > 0) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					SearchResultCollection c = runCoreSearchInternal(text);
					if (updateResult) {
						updateSearchResult(c, false);
					}
				}
			});
		} else {
			SearchResultCollection c = runCoreSearchInternal(text);
			if (updateResult) {
				updateSearchResult(c, false);
			}
		}
	}

	private SearchResultCollection runCoreSearchInternal(String text) {
		interruptedSearch = false;
		searching = true;
		return searchUICore.search(text, new ResultMatcher<SearchResult>() {

			SearchResultCollection regionResultCollection = null;
			SearchCoreAPI regionResultApi = null;
			List<SearchResult> results = new ArrayList<>();

			@Override
			public boolean publish(SearchResult object) {

				if (paused) {
					if (results.size() > 0) {
						getResultCollection().getCurrentSearchResults().addAll(results);
					}
					return false;
				}

				switch (object.objectType) {
					case SEARCH_API_FINISHED:
						final SearchCoreAPI searchApi = (SearchCoreAPI) object.object;

						final List<SearchResult> apiResults;
						final SearchPhrase phrase = object.requiredSearchPhrase;
						final SearchCoreAPI regionApi = regionResultApi;
						final SearchResultCollection regionCollection = regionResultCollection;

						final boolean hasRegionCollection = (searchApi == regionApi && regionCollection != null);
						if (hasRegionCollection) {
							apiResults = regionCollection.getCurrentSearchResults();
						} else {
							apiResults = results;
							searchUICore.sortSearchResults(phrase, apiResults);
						}

						regionResultApi = null;
						regionResultCollection = null;
						results = new ArrayList<>();

						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								if (!paused) {
									boolean appended = false;
									if (getResultCollection() == null || getResultCollection().getPhrase() != phrase) {
										setResultCollection(new SearchResultCollection(apiResults, phrase));
									} else {
										getResultCollection().getCurrentSearchResults().addAll(apiResults);
										appended = true;
									}
									if (!hasRegionCollection) {
										updateSearchResult(getResultCollection(), appended);
									}
								}
							}
						});
						break;
					case SEARCH_API_REGION_FINISHED:
						regionResultApi = (SearchCoreAPI) object.object;

						final List<SearchResult> regionResults = new ArrayList<>(results);
						final SearchPhrase regionPhrase = object.requiredSearchPhrase;
						searchUICore.sortSearchResults(regionPhrase, regionResults);

						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								if (!paused) {
									boolean appended = getResultCollection() != null && getResultCollection().getPhrase() == regionPhrase;
									regionResultCollection = new SearchResultCollection(regionResults, regionPhrase);
									if (appended) {
										List<SearchResult> res = new ArrayList<>(getResultCollection().getCurrentSearchResults());
										res.addAll(regionResults);
										SearchResultCollection resCollection = new SearchResultCollection(res, regionPhrase);
										updateSearchResult(resCollection, true);
									} else {
										updateSearchResult(regionResultCollection, false);
									}
								}
							}
						});
						break;
					case PARTIAL_LOCATION:
						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								foundPartialLocation = true;
								updateToolbarButton();
							}
						});
						break;
					default:
						results.add(object);
				}

				return false;
			}

			@Override
			public boolean isCancelled() {
				return paused;
			}
		});
	}

	public void completeQueryWithObject(SearchResult sr) {
		searchUICore.selectSearchResult(sr);
		String txt = searchUICore.getPhrase().getText(true);
		searchQuery = txt;
		searchEditText.setText(txt);
		searchEditText.setSelection(txt.length());
		updateToolbarButton();
		SearchSettings settings = searchUICore.getPhrase().getSettings();
		if(settings.getRadiusLevel() != 1){
			searchUICore.updateSettings(settings.setRadiusLevel(1));
		}
		runCoreSearch(txt, false);
	}

	private void addMoreButton() {
		if ((searchUICore.getPhrase().isUnknownSearchWordPresent() || searchUICore.getPhrase().isLastWord(ObjectType.POI_TYPE))
				&& searchUICore.getPhrase().getSettings().getRadiusLevel() < 7) {

			QuickSearchMoreListItem moreListItem =
					new QuickSearchMoreListItem(app, app.getString(R.string.search_POI_level_btn).toUpperCase(), new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (!interruptedSearch) {
								SearchSettings settings = searchUICore.getPhrase().getSettings();
								searchUICore.updateSettings(settings.setRadiusLevel(settings.getRadiusLevel() + 1));
							}
							runCoreSearch(searchQuery, false);
						}
					});

			if (!paused && mainSearchFragment != null) {
				mainSearchFragment.addListItem(moreListItem);
			}
		}
	}

	private void updateSearchResult(SearchResultCollection res, boolean appended) {

		if (!paused && mainSearchFragment != null) {
			List<QuickSearchListItem> rows = new ArrayList<>();
			if (res.getCurrentSearchResults().size() > 0) {
				for (final SearchResult sr : res.getCurrentSearchResults()) {
					rows.add(new QuickSearchListItem(app, sr));
				}
			}
			mainSearchFragment.updateListAdapter(rows, appended);
		}
	}

	public static boolean showInstance(final MapActivity mapActivity, final String searchQuery) {
		try {

			if (mapActivity.isActivityDestroyed()) {
				return false;
			}

			Bundle bundle = new Bundle();
			bundle.putString(QUICK_SEARCH_QUERY_KEY, searchQuery);
			QuickSearchDialogFragment fragment = new QuickSearchDialogFragment();
			fragment.setArguments(bundle);
			fragment.show(mapActivity.getSupportFragmentManager(), TAG);
			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void updateCompassValue(final float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			final Location location = this.location;
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					updateLocationUI(location, value);
				}
			});
		} else {
			heading = lastHeading;
		}
	}

	@Override
	public void updateLocation(final Location location) {
		this.location = location;
		final Float heading = this.heading;
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				updateLocationUI(location, heading);
			}
		});
	}

	private void updateLocationUI(Location location, Float heading) {
		LatLon latLon = null;
		if (location != null) {
			latLon = new LatLon(location.getLatitude(), location.getLongitude());
		}
		updateLocationUI(latLon, heading);
	}

	private void updateLocationUI(LatLon latLon, Float heading) {
		if (latLon != null && !paused) {
			if (mainSearchFragment != null && searchView.getVisibility() == View.VISIBLE) {
				mainSearchFragment.updateLocation(latLon, heading);
			} else if (historySearchFragment != null && viewPager.getCurrentItem() == 0) {
				historySearchFragment.updateLocation(latLon, heading);
			} else if (categoriesSearchFragment != null && viewPager.getCurrentItem() == 1) {
				categoriesSearchFragment.updateLocation(latLon, heading);
			}
		}
	}

	private void updateUseMapCenterUI() {
		if (!paused) {
			if (mainSearchFragment != null) {
				mainSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (historySearchFragment != null) {
				historySearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
			if (categoriesSearchFragment != null) {
				categoriesSearchFragment.getListAdapter().setUseMapCenter(useMapCenter);
			}
		}
	}

	public void enableSelectionMode(boolean selectionMode,int position) {
		historySearchFragment.setSelectionMode(selectionMode, position);
		tabToolbarView.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		buttonToolbarView.setVisibility(View.GONE);
		toolbar.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
		toolbarEdit.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
		viewPager.setSwipeLocked(selectionMode);
	}

	public void updateSelectionMode(List<QuickSearchListItem> selectedItems) {
		if (selectedItems.size() > 0) {
			String text = selectedItems.size() + " " + app.getString(R.string.shared_string_selected_lowercase);
			titleEdit.setText(text);
		} else {
			titleEdit.setText("");
		}
	}

	private void shareHistory(final List<HistoryEntry> historyEntries) {
		if (!historyEntries.isEmpty()) {
			final AsyncTask<Void, Void, GPXFile> exportTask = new AsyncTask<Void, Void, GPXFile>() {
				@Override
				protected GPXFile doInBackground(Void... params) {
					GPXFile gpx = new GPXFile();
					for (HistoryEntry h : historyEntries) {
						WptPt pt = new WptPt();
						pt.lat = h.getLat();
						pt.lon = h.getLon();
						pt.name = h.getName().getName();
						boolean hasTypeInDescription = !Algorithms.isEmpty(h.getName().getTypeName());
						if (hasTypeInDescription) {
							pt.desc = h.getName().getTypeName();
						}
						gpx.points.add(pt);
					}
					return gpx;
				}

				@Override
				protected void onPreExecute() {
					showProgressBar();
				}

				@Override
				protected void onPostExecute(GPXFile gpxFile) {
					hideProgressBar();
					File dir = new File(getActivity().getCacheDir(), "share");
					if (!dir.exists()) {
						dir.mkdir();
					}
					File dst = new File(dir, "History.gpx");
					GPXUtilities.writeGpxFile(dst, gpxFile, app);

					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, "History.gpx:\n\n\n" + GPXUtilities.asString(gpxFile, app));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_history_subject));
					sendIntent.putExtra(Intent.EXTRA_STREAM,
							FileProvider.getUriForFile(getActivity(),
									getActivity().getPackageName() + ".fileprovider", dst));
					sendIntent.setType("text/plain");
					startActivity(sendIntent);
				}
			};
			exportTask.execute();
		}
	}

	public class SearchFragmentPagerAdapter extends FragmentPagerAdapter {
		private final String[] fragments = new String[]{QuickSearchHistoryListFragment.class.getName(),
				QuickSearchCategoriesListFragment.class.getName()};
		private final int[] titleIds = new int[]{QuickSearchHistoryListFragment.TITLE,
				QuickSearchCategoriesListFragment.TITLE};
		private final String[] titles;

		public SearchFragmentPagerAdapter(FragmentManager fm, Resources res) {
			super(fm);
			titles = new String[titleIds.length];
			for (int i = 0; i < titleIds.length; i++) {
				titles[i] = res.getString(titleIds[i]);
			}
		}

		@Override
		public int getCount() {
			return fragments.length;
		}

		@Override
		public Fragment getItem(int position) {
			return Fragment.instantiate(QuickSearchDialogFragment.this.getContext(), fragments[position]);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
		}
	}

	public static class QuickSearchHistoryListFragment extends QuickSearchListFragment {
		public static final int TITLE = R.string.shared_string_history;
		private boolean selectionMode;

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.HISTORY;
		}

		public boolean isSelectionMode() {
			return selectionMode;
		}

		public void setSelectionMode(boolean selectionMode, int position) {
			this.selectionMode = selectionMode;
			getListAdapter().setSelectionMode(selectionMode, position);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					if (selectionMode) {
						return false;
					} else {
						getDialogFragment().enableSelectionMode(true, position - getListView().getHeaderViewsCount());
						return true;
					}
				}
			});
			getListAdapter().setSelectionListener(new QuickSearchListAdapter.OnSelectionListener() {
				@Override
				public void onUpdateSelectionMode(List<QuickSearchListItem> selectedItems) {
					getDialogFragment().updateSelectionMode(selectedItems);
				}

				@Override
				public void reloadData() {
					getDialogFragment().reloadHistory();
				}
			});
		}

		@Override
		public void onListItemClick(ListView l, View view, int position, long id) {
			if (selectionMode) {
				CheckBox ch = (CheckBox) view.findViewById(R.id.toggle_item);
				ch.setChecked(!ch.isChecked());
				getListAdapter().toggleCheckbox(position - l.getHeaderViewsCount(), ch);
			} else {
				super.onListItemClick(l, view, position, id);
			}
		}
	}

	public static class QuickSearchCategoriesListFragment extends QuickSearchListFragment {
		public static final int TITLE = R.string.search_categories;

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.CATEGORIES;
		}
	}

	public static class QuickSearchMainListFragment extends QuickSearchListFragment {

		@Override
		public SearchListFragmentType getType() {
			return SearchListFragmentType.MAIN;
		}
	}
}
