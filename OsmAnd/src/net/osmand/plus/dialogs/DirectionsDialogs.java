package net.osmand.plus.dialogs;

import net.osmand.data.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.Item;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class DirectionsDialogs {
	
	public static void directionsToDialogAndLaunchMap(final Activity act, final double lat, final double lon, final String name) {
		final OsmandApplication ctx = (OsmandApplication) act.getApplication();
		final TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (targetPointsHelper.getIntermediatePoints().size() > 0) {
			Builder builder = new AlertDialog.Builder(act);
			builder.setTitle(R.string.new_directions_point_dialog);
			builder.setItems(
					new String[] { act.getString(R.string.keep_intermediate_points),
							act.getString(R.string.clear_intermediate_points)},
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 1) {
								targetPointsHelper.clearPointToNavigate(false);
							}
							ctx.getSettings().navigateDialog();
							targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
							MapActivity.launchMapActivityMoveToTop(act);
						}
					});
			builder.show();
		} else {
			ctx.getSettings().navigateDialog();
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			MapActivity.launchMapActivityMoveToTop(act);
		}
	}
	
	public static void createDirectionsActions(final ContextMenuAdapter qa , final LatLon location, final Object obj, final String name, 
    		final int z, final Activity activity, final boolean saveHistory) {
		createDirectionsActions(qa, location, obj, name, z, activity, saveHistory, true);
	}
	
	public static void createDirectionsActions(final ContextMenuAdapter qa , final LatLon location, final Object obj, final String name, 
    		final int z, final Activity activity, final boolean saveHistory, boolean favorite) {

		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		final TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		
		
		Item dir = qa.item(R.string.context_menu_item_directions_to).icons(
				R.drawable.ic_action_gdirections_dark, R.drawable.ic_action_gdirections_light);
		dir.listen(
				new OnContextMenuClick() {
					
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						directionsToDialogAndLaunchMap(activity, location.getLatitude(), location.getLongitude(), name);
						return true;
					}
				}).reg();
		Item intermediate; 
		if (targetPointsHelper.getPointToNavigate() != null) {
			intermediate = qa.item(R.string.context_menu_item_intermediate_point).icons(
					R.drawable.ic_action_flage_dark,R.drawable.ic_action_flage_light);
		} else {
			intermediate = qa.item(R.string.context_menu_item_destination_point).icons(
					R.drawable.ic_action_flag_dark, R.drawable.ic_action_flag_light);
		}
		intermediate.listen(new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				addWaypointDialogAndLaunchMap(activity, location.getLatitude(), location.getLongitude(), name);
				return true;
			}
		}).reg();

		Item showOnMap = qa.item(R.string.show_poi_on_map).icons(
				R.drawable.ic_action_marker_dark, R.drawable.ic_action_marker_light );
		showOnMap.listen(
				new OnContextMenuClick() {
					
					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						app.getSettings().setMapLocationToShow(location.getLatitude(), location.getLongitude(), z, saveHistory ? name : null, name,
								obj); //$NON-NLS-1$
						MapActivity.launchMapActivityMoveToTop(activity);
						return true;
					}
				}).reg();
		if (favorite) {
			Item addToFavorite = qa.item(R.string.add_to_favourite).icons(
					R.drawable.ic_action_fav_dark, R.drawable.ic_action_fav_light);
			addToFavorite.listen(new OnContextMenuClick() {

				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
					Bundle args = new Bundle();
					Dialog dlg = FavoriteDialogs.createAddFavouriteDialog(activity, args);
					dlg.show();
					FavoriteDialogs.prepareAddFavouriteDialog(activity, dlg, args, location.getLatitude(), location.getLongitude(),
							name);
					return true;
				}
			}).reg();
		}
	}

	public static void addWaypointDialogAndLaunchMap(final Activity act, final double lat, final double lon, final String name) {
		final OsmandApplication ctx = (OsmandApplication) act.getApplication();
		final TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (targetPointsHelper.getPointToNavigate() != null) {
			Builder builder = new AlertDialog.Builder(act);
			builder.setTitle(R.string.new_destination_point_dialog);
			builder.setItems(
					new String[] { act.getString(R.string.replace_destination_point),
							act.getString(R.string.keep_and_add_destination_point),
							act.getString(R.string.add_as_first_destination_point), act.getString(R.string.add_as_last_destination_point) },
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
							} else if (which == 1) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, 
										targetPointsHelper.getIntermediatePoints().size() + 1, name);
							} else if (which == 2) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, 0, name);
							} else {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, targetPointsHelper.getIntermediatePoints().size(), name);
							}
							MapActivity.launchMapActivityMoveToTop(act);
						}
					});
			builder.show();
		} else {
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			MapActivity.launchMapActivityMoveToTop(act);
		}
	}
}
