package net.osmand.plus.track.helpers;

import static net.osmand.router.network.NetworkRouteContext.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.router.network.NetworkRouteSelector;
import net.osmand.router.network.NetworkRouteSelector.INetworkRouteSelection;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class NetworkRouteSelectionTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private static final Log log = PlatformUtil.getLog(NetworkRouteSelectionTask.class);

	private final OsmandApplication app;

	private final QuadRect quadRect;
	private final NetworkRouteSegment routeSegment;
	private final INetworkRouteSelection callback;

	public NetworkRouteSelectionTask(@NonNull FragmentActivity activity,
	                                 @NonNull NetworkRouteSegment routeSegment,
	                                 @NonNull QuadRect quadRect,
	                                 @Nullable INetworkRouteSelection callback) {

		super(activity);
		this.app = (OsmandApplication) activity.getApplication();
		this.routeSegment = routeSegment;
		this.quadRect = quadRect;
		this.callback = callback;
	}

	@Override
	protected void onPreExecute() {
		FragmentActivity activity = activityRef.get();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).showProgressBarForNetwork();
		}
	}

	@Override
	protected GPXFile doInBackground(Void... voids) {
		BinaryMapIndexReader[] readers = app.getResourceManager().getRoutingMapFiles();
		NetworkRouteSelectorFilter selectorFilter = new NetworkRouteSelectorFilter();
		NetworkRouteSelector routeSelector = new NetworkRouteSelector(readers, selectorFilter, callback);
		RouteKey routeKey = routeSegment.routeKey;
		if (routeKey != null) {
			selectorFilter.keyFilter = Collections.singleton(routeKey);
			try {
				Map<RouteKey, GPXFile> routes = routeSelector.getRoutes(quadRect, true, routeKey);
				if (callback.isCancelled()) {
					routes.clear();
				}
				if (!Algorithms.isEmpty(routes)) {
					return routes.values().iterator().next();
				}
			} catch (IOException e) {
				log.error(e);
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(GPXFile gpxFile) {
		FragmentActivity activity = activityRef.get();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).hideProgressBarForNetwork();
		}
		if (callback != null) {
			callback.processResult(gpxFile);
		}
	}
}
