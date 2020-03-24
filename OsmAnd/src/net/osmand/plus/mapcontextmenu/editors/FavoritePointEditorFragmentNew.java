package net.osmand.plus.mapcontextmenu.editors;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.util.Algorithms;

import java.util.LinkedHashSet;
import java.util.Set;

public class FavoritePointEditorFragmentNew extends PointEditorFragmentNew {

	@Nullable
	private FavoritePointEditor editor;
	@Nullable
	private FavouritePoint favorite;
	@Nullable
	private FavoriteGroup group;
	private int color;
	private int iconId;
	@NonNull
	private FavouritePoint.BackgroundType backgroundType = FavouritePoint.BackgroundType.CIRCLE;

	@Nullable
	FavouritesDbHelper helper;

	private boolean autoFill;
	private boolean saved;
	private int defaultColor;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			helper = mapActivity.getMyApplication().getFavorites();
			editor = mapActivity.getContextMenu().getFavoritePointEditor();
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		defaultColor = getResources().getColor(R.color.color_favorite);

		FavoritePointEditor editor = getFavoritePointEditor();
		FavouritesDbHelper helper = getHelper();
		if (editor != null && helper != null) {
			FavouritePoint favorite = editor.getFavorite();
			this.favorite = favorite;
			this.group = helper.getGroup(favorite);
			this.color = favorite.getColor();
			this.backgroundType = favorite.getBackgroundType();
			this.iconId = favorite.getIconId();
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		FavoritePointEditor editor = getFavoritePointEditor();
		if (view != null) {
			View replaceButton = view.findViewById(R.id.button_replace_container);
			replaceButton.setVisibility(View.VISIBLE);
			replaceButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					replacePressed();
				}
			});
			if (editor != null && editor.isNew()) {
				ImageView toolbarAction = (ImageView) view.findViewById(R.id.toolbar_action);
				toolbarAction.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						replacePressed();
					}
				});
			}
		}
		return view;
	}

	private void replacePressed() {
		Bundle args = new Bundle();
		args.putSerializable(FavoriteDialogs.KEY_FAVORITE, getFavorite());
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FavoriteDialogs.createReplaceFavouriteDialog(activity, args);
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (autoFill) {

//			String name = favorite.getName() != null && !favorite.getName().isEmpty() ?
//					favorite.getName() : getString(R.string.favorite_empty_place_name);
//
//			String tostText = name + getString(R.string.favorite_autofill_toast_text) + group.name;
//
//			Toast.makeText(getContext(), tostText, Toast.LENGTH_SHORT).show();

			save(true);
		}
	}

	@Override
	public PointEditor getEditor() {
		return editor;
	}

	public FavoritePointEditor getFavoritePointEditor() {
		return editor;
	}

	@Nullable
	public FavouritePoint getFavorite() {
		return favorite;
	}

	@Nullable
	public FavoriteGroup getGroup() {
		return group;
	}

	@Nullable
	public FavouritesDbHelper getHelper() {
		return helper;
	}

	@Override
	public String getToolbarTitle() {
		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null) {
			if (editor.isNew()) {
				return getString(R.string.favourites_context_menu_add);
			} else {
				return getString(R.string.favourites_context_menu_edit);
			}
		}
		return "";
	}

	@Override
	public void setCategory(String name, int color) {
		FavouritesDbHelper helper = getHelper();
		if (helper != null) {
			FavoriteGroup group = helper.getGroup(FavoriteGroup.convertDisplayNameToGroupIdName(requireContext(), name));
			this.group = group;
			super.setCategory(name, group != null ? group.getColor() : 0);
		}
	}

	@Override
	public void setColor(int color) {
		this.color = color;
	}

	@Override
	public void setBackgroundType(@NonNull FavouritePoint.BackgroundType backgroundType) {
		this.backgroundType = backgroundType;
	}

	@Override
	public void setIcon(int iconId) {
		this.iconId = iconId;
	}

	@Override
	protected String getDefaultCategoryName() {
		return getString(R.string.shared_string_favorites);
	}

	public static void showInstance(@NonNull MapActivity mapActivity) {
		FavoritePointEditor editor = mapActivity.getContextMenu().getFavoritePointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		if (editor != null) {
			FavoritePointEditorFragmentNew fragment = new FavoritePointEditorFragmentNew();
			mapActivity.getSupportFragmentManager().beginTransaction()
					//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commitAllowingStateLoss();
		}
	}

	public static void showAutoFillInstance(final MapActivity mapActivity, boolean autoFill) {
		FavoritePointEditor editor = mapActivity.getContextMenu().getFavoritePointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		FavoritePointEditorFragmentNew fragment = new FavoritePointEditorFragmentNew();
		fragment.autoFill = autoFill;

		if (editor != null) {
			mapActivity.getSupportFragmentManager().beginTransaction()
					//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commit();
		}
	}

	@Override
	protected boolean wasSaved() {
		return saved;
	}

	@Override
	protected void save(final boolean needDismiss) {
		final FavouritePoint favorite = getFavorite();
		if (favorite != null) {
			final FavouritePoint point = new FavouritePoint(favorite.getLatitude(), favorite.getLongitude(),
					getNameTextValue(), getCategoryTextValue());
			point.setDescription(getDescriptionTextValue());
			point.setColor(color);
			point.setBackgroundType(backgroundType);
			point.setIconId(iconId);
			AlertDialog.Builder builder = FavouritesDbHelper.checkDuplicates(point, helper, getMapActivity());

			if (isChanged(favorite, point)) {

				if (needDismiss) {
					dismiss(false);
				}
				return;
			}

			if (builder != null && !autoFill) {
				builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doSave(favorite, point.getName(), point.getCategory(), point.getDescription(),
								point.getColor(), point.getBackgroundType(), point.getIconId(), needDismiss);
					}
				});
				builder.create().show();
			} else {
				doSave(favorite, point.getName(), point.getCategory(), point.getDescription(),
						point.getColor(), point.getBackgroundType(), point.getIconId(), needDismiss);
			}
			saved = true;
		}
	}

	private boolean isChanged(FavouritePoint favorite, FavouritePoint point) {
		return favorite.getColor() == point.getColor() &&
				favorite.getIconId() == point.getIconId() &&
				favorite.getName().equals(point.getName()) &&
				favorite.getCategory().equals(point.getCategory()) &&
				favorite.getBackgroundType().equals(point.getBackgroundType()) &&
				Algorithms.stringsEqual(favorite.getDescription(), point.getDescription());
	}

	private void doSave(FavouritePoint favorite, String name, String category, String description,
	                    @ColorInt int color, FavouritePoint.BackgroundType backgroundType, @DrawableRes int iconId, boolean needDismiss) {
		FavouritesDbHelper helper = getHelper();
		FavoritePointEditor editor = getFavoritePointEditor();
		if (editor != null && helper != null) {
			if (editor.isNew()) {
				doAddFavorite(name, category, description, color, backgroundType, iconId);
			} else {
				favorite.setColor(color);
				favorite.setBackgroundType(backgroundType);
				favorite.setIconId(iconId);
				helper.editFavouriteName(favorite, name, category, description);
			}
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		mapActivity.refreshMap();
		if (needDismiss) {
			dismiss(false);
		}

		MapContextMenu menu = mapActivity.getContextMenu();
		LatLon latLon = new LatLon(favorite.getLatitude(), favorite.getLongitude());
		if (menu.getLatLon() != null && menu.getLatLon().equals(latLon)) {
			menu.update(latLon, favorite.getPointDescription(mapActivity), favorite);
		}
	}

	private void doAddFavorite(String name, String category, String description, @ColorInt int color,
	                           FavouritePoint.BackgroundType backgroundType, @DrawableRes int iconId) {
		OsmandApplication app = getMyApplication();
		FavouritesDbHelper helper = getHelper();
		FavouritePoint favorite = getFavorite();
		if (app != null && favorite != null && helper != null) {
			favorite.setName(name);
			favorite.setCategory(category);
			favorite.setDescription(description);
			favorite.setColor(color);
			favorite.setBackgroundType(backgroundType);
			favorite.setIconId(iconId);
			app.getSettings().LAST_FAV_CATEGORY_ENTERED.set(category);
			helper.addFavourite(favorite);
		}
	}

	@Override
	protected void delete(final boolean needDismiss) {
		FragmentActivity activity = getActivity();
		final FavouritePoint favorite = getFavorite();
		if (activity != null && favorite != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage(getString(R.string.favourites_remove_dialog_msg, favorite.getName()));
			builder.setNegativeButton(R.string.shared_string_no, null);
			builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					FavouritesDbHelper helper = getHelper();
					if (helper != null) {
						helper.deleteFavourite(favorite);
						saved = true;
						if (needDismiss) {
							dismiss(true);
						} else {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity != null) {
								mapActivity.refreshMap();
							}
						}
					}
				}
			});
			builder.create().show();
		}
	}

	@Override
	public String getNameInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite != null ? favorite.getName() : "";
	}

	@Override
	public String getCategoryInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite == null || favorite.getCategory().length() == 0 ? getDefaultCategoryName() : favorite.getCategoryDisplayName(requireContext());
	}

	@Override
	public String getDescriptionInitValue() {
		FavouritePoint favorite = getFavorite();
		return favorite != null ? favorite.getDescription() : "";
	}

	@Override
	public Drawable getNameIcon() {
		FavouritePoint favorite = getFavorite();
		FavouritePoint point = null;
		if (favorite != null) {
			point = new FavouritePoint(favorite);
			point.setColor(getPointColor());
			point.setBackgroundType(backgroundType);
			point.setIconId(iconId);
		}
		return FavoriteImageDrawable.getOrCreate(getMapActivity(), getPointColor(), false, point);
	}

	@Override
	public Drawable getCategoryIcon() {
		return getPaintedIcon(R.drawable.ic_action_folder_stroke, getPointColor());
	}

	@Override
	public int getDefaultColor() {
		return defaultColor;
	}

	@Override
	public int getPointColor() {
		FavouritePoint favorite = getFavorite();
		int color = favorite != null ? getColor() : 0;
		FavoriteGroup group = getGroup();
		if (group != null && color == 0) {
			color = group.getColor();
		}
		if (color == 0) {
			color = defaultColor;
		}
		return color;
	}

	@Override
	@NonNull
	public FavouritePoint.BackgroundType getBackgroundType() {
		return backgroundType;
	}

	@Override
	public int getIconId() {
		return iconId;
	}

	private int getColor() {
		return color;
	}

	@Override
	public Set<String> getCategories() {
		Set<String> categories = new LinkedHashSet<>();
		for (FavouritesDbHelper.FavoriteGroup fg : getHelper().getFavoriteGroups()) {
			categories.add(fg.getDisplayName(getMyApplication()));
		}
		return categories;
	}

	@Override
	public int getCategoryPoints(String category) {
		for (FavouritesDbHelper.FavoriteGroup fg : getHelper().getFavoriteGroups()) {
			if (fg.getDisplayName(getMyApplication()).equals(category)) {
				return fg.getPoints().size();
			}
		}
		return 0;
	}

	@Override
	public int getCategoryColor(String category) {
		for (FavouritesDbHelper.FavoriteGroup fg : getHelper().getFavoriteGroups()) {
			if (fg.getDisplayName(getMyApplication()).equals(category)) {
				return fg.getColor();
			}
		}
		return defaultColor;
	}
}
