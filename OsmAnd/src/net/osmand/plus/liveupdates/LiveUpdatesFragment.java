package net.osmand.plus.liveupdates;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LiveUpdatesFragment extends Fragment {
	public static final String TITILE = "Live Updates";
	public static final String LIVE_UPDATES_ON_POSTFIX = "_live_updates_on";
	public static final Comparator<LocalIndexInfo> LOCAL_INDEX_INFO_COMPARATOR = new Comparator<LocalIndexInfo>() {
		@Override
		public int compare(LocalIndexInfo lhs, LocalIndexInfo rhs) {
			return lhs.getName().compareTo(rhs.getName());
		}
	};
	private ExpandableListView listView;
	private LocalIndexesAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live_updates, container, false);
		listView = (ExpandableListView) view.findViewById(android.R.id.list);
		View header = inflater.inflate(R.layout.live_updates_header, listView, false);

		final OsmandSettings settings = getMyActivity().getMyApplication().getSettings();

		final TextView onOffTextView = (TextView) header.findViewById(R.id.onOffTextView);
		int liveUpdatesStateId = settings.IS_LIVE_UPDATES_ON.get()
				? R.string.shared_string_on : R.string.shared_string_off;
		onOffTextView.setText(liveUpdatesStateId);

		SwitchCompat liveUpdatesSwitch = (SwitchCompat) header.findViewById(R.id.liveUpdatesSwitch);
		liveUpdatesSwitch.setChecked(settings.IS_LIVE_UPDATES_ON.get());
		liveUpdatesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				settings.IS_LIVE_UPDATES_ON.set(isChecked);
				int liveUpdatesStateId = isChecked ? R.string.shared_string_on
						: R.string.shared_string_off;
				onOffTextView.setText(liveUpdatesStateId);

			}
		});

		listView.addHeaderView(header);
		adapter = new LocalIndexesAdapter(this);
		listView.setAdapter(adapter);
		new LoadLocalIndexTask(adapter, this).execute();
		return view;
	}

	private AbstractDownloadActivity getMyActivity() {
		return (AbstractDownloadActivity) getActivity();
	}

	public void notifyLiveUpdatesChanged() {
		adapter.notifyLiveUpdatesChanged();
	}

	protected class LocalIndexesAdapter extends OsmandBaseExpandableListAdapter {
		final ArrayList<LocalIndexInfo> dataShouldUpdate = new ArrayList<>();
		final ArrayList<LocalIndexInfo> dataShouldNotUpdate = new ArrayList<>();
		final LiveUpdatesFragment fragment;
		final Context ctx;

		public LocalIndexesAdapter(LiveUpdatesFragment fragment) {
			this.fragment = fragment;
			ctx = fragment.getActivity();
		}

		public void add(LocalIndexInfo info) {
			OsmandSettings.CommonPreference<Boolean> preference =
					preferenceForLocalIndex(LIVE_UPDATES_ON_POSTFIX, info);
			if (preference.get()) {
				dataShouldUpdate.add(info);
			} else {
				dataShouldNotUpdate.add(info);
			}
		}

		public void notifyLiveUpdatesChanged() {
			for (LocalIndexInfo localIndexInfo : dataShouldUpdate) {
				OsmandSettings.CommonPreference<Boolean> preference =
						preferenceForLocalIndex(LIVE_UPDATES_ON_POSTFIX, localIndexInfo);
				if (!preference.get()) {
					dataShouldUpdate.remove(localIndexInfo);
					dataShouldNotUpdate.add(localIndexInfo);
				}
			}
			for (LocalIndexInfo localIndexInfo : dataShouldNotUpdate) {
				OsmandSettings.CommonPreference<Boolean> preference =
						preferenceForLocalIndex(LIVE_UPDATES_ON_POSTFIX, localIndexInfo);
				if (preference.get()) {
					dataShouldUpdate.add(localIndexInfo);
					dataShouldNotUpdate.remove(localIndexInfo);
				}
			}
			notifyDataSetChanged();
		}

		public void sort() {
			Collections.sort(dataShouldUpdate, LOCAL_INDEX_INFO_COMPARATOR);
			Collections.sort(dataShouldNotUpdate, LOCAL_INDEX_INFO_COMPARATOR);
		}

		@Override
		public LocalIndexInfo getChild(int groupPosition, int childPosition) {
			if (groupPosition == 0) {
				return dataShouldUpdate.get(childPosition);
			} else if (groupPosition == 1) {
				return dataShouldNotUpdate.get(childPosition);
			} else {
				throw new IllegalArgumentException("unexpected group position:" + groupPosition);
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			// it would be unusable to have 10000 local indexes
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {
			LocalFullMapsViewHolder viewHolder;
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				convertView = inflater.inflate(R.layout.local_index_list_item, parent, false);
				viewHolder = new LocalFullMapsViewHolder(convertView, fragment);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (LocalFullMapsViewHolder) convertView.getTag();
			}
			viewHolder.bindLocalIndexInfo(getChild(groupPosition, childPosition));
			return convertView;
		}


		private String getNameToDisplay(LocalIndexInfo child) {
			String mapName = FileNameTranslationHelper.getFileName(ctx,
					fragment.getMyActivity().getMyApplication().getResourceManager().getOsmandRegions(),
					child.getFileName());
			return mapName;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			String group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(ctx);
				v = inflater.inflate(R.layout.download_item_list_section, parent, false);
			}
			TextView nameView = ((TextView) v.findViewById(R.id.section_name));
			nameView.setText(group);

			v.setOnClickListener(null);

			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = ctx.getTheme();
			theme.resolveAttribute(R.attr.ctx_menu_info_view_bg, typedValue, true);
			v.setBackgroundColor(typedValue.data);
			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			if (groupPosition == 0) {
				return dataShouldUpdate.size();
			} else if (groupPosition == 1) {
				return dataShouldNotUpdate.size();
			} else {
				throw new IllegalArgumentException("unexpected group position:" + groupPosition);
			}
		}

		@Override
		public String getGroup(int groupPosition) {
			if (groupPosition == 0) {
				return getString(R.string.live_updates_on);
			} else if (groupPosition == 1) {
				return getString(R.string.live_updates_off);
			} else {
				throw new IllegalArgumentException("unexpected group position:" + groupPosition);
			}
		}

		@Override
		public int getGroupCount() {
			return 2;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		private OsmandSettings.CommonPreference<Boolean> preferenceForLocalIndex(String idPostfix,
																				 LocalIndexInfo item) {
			final OsmandApplication myApplication = fragment.getMyActivity().getMyApplication();
			final OsmandSettings settings = myApplication.getSettings();
			final String settingId = item.getFileName() + idPostfix;
			return settings.registerBooleanPreference(settingId, false);
		}
	}

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	void runLiveUpdate(final LocalIndexInfo info) {
		final String fnExt = Algorithms.getFileNameWithoutExtension(new File(info.getFileName()));
		new AsyncTask<Object, Object, IncrementalChangesManager.IncrementalUpdateList>() {

			protected void onPreExecute() {
				getMyActivity().setSupportProgressBarIndeterminateVisibility(true);

			}

			@Override
			protected IncrementalChangesManager.IncrementalUpdateList doInBackground(Object... params) {
				final OsmandApplication myApplication = getMyActivity().getMyApplication();
				IncrementalChangesManager cm = myApplication.getResourceManager().getChangesManager();
				return cm.getUpdatesByMonth(fnExt);
			}

			protected void onPostExecute(IncrementalChangesManager.IncrementalUpdateList result) {
				getMyActivity().setSupportProgressBarIndeterminateVisibility(false);
				if (result.errorMessage != null) {
					Toast.makeText(getActivity(), result.errorMessage, Toast.LENGTH_SHORT).show();
				} else {
					List<IncrementalChangesManager.IncrementalUpdate> ll = result.getItemsForUpdate();
					if (ll.isEmpty()) {
						Toast.makeText(getActivity(), R.string.no_updates_available, Toast.LENGTH_SHORT).show();
					} else {
						int i = 0;
						IndexItem[] is = new IndexItem[ll.size()];
						for (IncrementalChangesManager.IncrementalUpdate iu : ll) {
							IndexItem ii = new IndexItem(iu.fileName, "Incremental update", iu.timestamp, iu.sizeText,
									iu.contentSize, iu.containerSize, DownloadActivityType.LIVE_UPDATES_FILE);
							is[i++] = ii;
						}
						getMyActivity().startDownload(is);
					}
				}

			}

		}.execute(new Object[]{fnExt});
	}

	LocalIndexInfo getLocalIndexInfo(int groupPosition, int childPosition) {
		return adapter.getChild(groupPosition, childPosition);
	}

	private static class LocalFullMapsViewHolder {
		private final ImageView icon;
		private final TextView nameTextView;
		private final TextView descriptionTextView;
		private final ImageButton options;
		private final LiveUpdatesFragment fragment;

		private LocalFullMapsViewHolder(View view, LiveUpdatesFragment context) {
			icon = (ImageView) view.findViewById(R.id.icon);
			nameTextView = (TextView) view.findViewById(R.id.nameTextView);
			descriptionTextView = (TextView) view.findViewById(R.id.descriptionTextView);
			options = (ImageButton) view.findViewById(R.id.options);
			this.fragment = context;
		}

		public void bindLocalIndexInfo(final LocalIndexInfo item) {
			nameTextView.setText(item.getName());
			descriptionTextView.setText(item.getDescription());
			OsmandApplication context = fragment.getMyActivity().getMyApplication();
			icon.setImageDrawable(context.getIconsCache().getContentIcon(R.drawable.ic_map));
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final FragmentManager fragmentManager = fragment.getChildFragmentManager();
					SettingsDialogFragment.createInstance(item).show(fragmentManager, "settings");
				}
			});
		}
	}

	public static class LoadLocalIndexTask
			extends AsyncTask<Void, LocalIndexInfo, List<LocalIndexInfo>>
			implements AbstractLoadLocalIndexTask {

		private List<LocalIndexInfo> result;
		private LocalIndexesAdapter adapter;
		private LiveUpdatesFragment fragment;

		public LoadLocalIndexTask(LocalIndexesAdapter adapter,
								  LiveUpdatesFragment fragment) {
			this.adapter = adapter;
			this.fragment = fragment;
		}

		@Override
		protected List<LocalIndexInfo> doInBackground(Void... params) {
			LocalIndexHelper helper = new LocalIndexHelper(fragment.getMyActivity().getMyApplication());
			return helper.getLocalIndexData(this);
		}

		@Override
		public void loadFile(LocalIndexInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onProgressUpdate(LocalIndexInfo... values) {
			for (LocalIndexInfo localIndexInfo : values) {
				if (localIndexInfo.getType() == LocalIndexHelper.LocalIndexType.MAP_DATA) {
					adapter.add(localIndexInfo);
				}
			}
			adapter.notifyDataSetChanged();
			fragment.expandAllGroups();
		}

		@Override
		protected void onPostExecute(List<LocalIndexInfo> result) {
			this.result = result;
			adapter.sort();
		}
	}
}
