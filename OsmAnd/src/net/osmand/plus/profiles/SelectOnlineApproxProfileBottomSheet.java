package net.osmand.plus.profiles;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.onlinerouting.ui.OnlineRoutingEngineFragment;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfilesGroup;
import net.osmand.plus.profiles.data.RoutingDataObject;
import net.osmand.plus.profiles.data.RoutingDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class SelectOnlineApproxProfileBottomSheet extends SelectProfileBottomSheet {

	public static final String NETWORK_KEY = "network_key";
	private RoutingDataUtils dataUtils;
	private List<ProfilesGroup> profileGroups = new ArrayList<>();
	private boolean isNetwork;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			isNetwork = args.getBoolean(NETWORK_KEY);
		}
		super.onCreate(savedInstanceState);
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable Fragment target,
	                                @Nullable ApplicationMode appMode,
	                                @Nullable String selectedItemKey,
	                                boolean isNetwork, boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			SelectOnlineApproxProfileBottomSheet fragment = new SelectOnlineApproxProfileBottomSheet();
			Bundle args = new Bundle();
			args.putString(SELECTED_KEY, selectedItemKey);
			args.putBoolean(NETWORK_KEY, isNetwork);
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(@Nullable Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_base_profile_dialog_title)));
		addCheckableItem(R.string.shared_string_none, Algorithms.isEmpty(selectedItemKey) && !isNetwork, v -> {
			Bundle args = new Bundle();
			args.putBoolean(NETWORK_KEY, false);
			args.putString(PROFILE_KEY_ARG, "");
			Fragment target = getTargetFragment();
			if (target instanceof OnSelectProfileCallback) {
				((OnSelectProfileCallback) target).onProfileSelected(args);
			}
			dismiss();
		});
		addCheckableItem(R.string.network_provider, isNetwork, v -> {
			Bundle args = new Bundle();
			args.putBoolean(NETWORK_KEY, true);
			args.putString(PROFILE_KEY_ARG, "");
			Fragment target = getTargetFragment();
			if (target instanceof OnlineRoutingEngineFragment) {
				((OnlineRoutingEngineFragment) target).onProfileSelected(args);
			}
			dismiss();
		});
		createProfilesList();
	}

	private void createProfilesList() {
		for (ProfilesGroup group : profileGroups) {
			List<RoutingDataObject> items = group.getProfiles();
			if (!Algorithms.isEmpty(items)) {
				addGroupHeader(group.getTitle(), group.getDescription(app, nightMode));
				for (RoutingDataObject item : items) {
					addProfileItem(item);
				}
				addDivider();
			}
		}
	}

	@Override
	protected void refreshProfiles() {
		profileGroups.clear();
		profileGroups = getDataUtils().getOfflineProfiles();
	}

	@Override
	protected int getIconColor(ProfileDataObject profile) {
		int iconColorResId = isSelected(profile) ? getActiveColorId() : getDefaultIconColorId();
		return ContextCompat.getColor(app, iconColorResId);
	}

	private RoutingDataUtils getDataUtils() {
		if (dataUtils == null) {
			dataUtils = new RoutingDataUtils(app);
		}
		return dataUtils;
	}
}
