package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.nearbyplaces.NearbyPlacesHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.NearbyPlacesTileProvider;
import net.osmand.wiki.WikiCoreHelper;
import net.osmand.wiki.WikiImage;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class NearbyPlacesLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final int START_ZOOM = 6;
	private static final Log LOG = PlatformUtil.getLog(NearbyPlacesLayer.class);
	public static final String LOAD_NEARBY_IMAGES_TAG = "load_nearby_images";

	protected List<NearbyPlacePoint> cache = new ArrayList<>();
	private boolean showNearbyPoints;
	private boolean nightMode;

	private final List<Target> imageLoadingTargets = new ArrayList<>();
	private Bitmap cachedSmallIconBitmap;

	public CustomMapObjects<NearbyPlacePoint> customObjectsDelegate = new OsmandMapLayer.CustomMapObjects<>();

	private NearbyPlacesTileProvider nearbyPlacesMapLayerProvider;

	public NearbyPlacesLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		nightMode = getApplication().getDaynightHelper().isNightMode();
		recreateBitmaps();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	private void recreateBitmaps() {
		cachedSmallIconBitmap = PointImageUtils.createSmallPointBitmap(getApplication());
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		recreateBitmaps();
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearNearbyPoints();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		boolean nightMode = settings != null && settings.isNightMode();
		boolean nightModeChanged = this.nightMode != nightMode;
		this.nightMode = nightMode;
		boolean showNearbyPoints = !customObjectsDelegate.getMapObjects().isEmpty();
		boolean showNearbyPlacesChanged = this.showNearbyPoints != showNearbyPoints;
		this.showNearbyPoints = showNearbyPoints;
		if (hasMapRenderer()) {
			if ((mapActivityInvalidated || mapRendererChanged
					|| nightModeChanged || showNearbyPlacesChanged
					|| customObjectsDelegate.isChanged())) {
				showNearbyPoints();
				customObjectsDelegate.acceptChanges();
				mapRendererChanged = false;
			}
		} else {
			cache.clear();
			if (showNearbyPoints && tileBox.getZoom() >= START_ZOOM) {
				float iconSize = getIconSize(view.getApplication());
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				QuadRect latLonBounds = tileBox.getLatLonBounds();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				drawPoints(customObjectsDelegate.getMapObjects(), latLonBounds, false, tileBox, boundIntersections, iconSize, canvas,
						fullObjectsLatLon, smallObjectsLatLon);
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		mapActivityInvalidated = false;
	}

	private void drawPoints(List<NearbyPlacePoint> pointsToDraw, QuadRect latLonBounds, boolean synced, RotatedTileBox tileBox,
	                        QuadTree<QuadRect> boundIntersections, float iconSize, Canvas canvas,
	                        List<LatLon> fullObjectsLatLon, List<LatLon> smallObjectsLatLon) {
		List<NearbyPlacePoint> fullObjects = new ArrayList<>();
		Paint pointPaint = new Paint();
		if (cachedSmallIconBitmap == null) {
			cachedSmallIconBitmap = PointImageUtils.createSmallPointBitmap(getApplication());
		}
		for (NearbyPlacePoint nearbyPoint : pointsToDraw) {
			double lat = nearbyPoint.getLatitude();
			double lon = nearbyPoint.getLongitude();
			if (lat >= latLonBounds.bottom && lat <= latLonBounds.top
					&& lon >= latLonBounds.left && lon <= latLonBounds.right) {
				cache.add(nearbyPoint);
				float x = tileBox.getPixXFromLatLon(lat, lon);
				float y = tileBox.getPixYFromLatLon(lat, lon);
				if (intersects(boundIntersections, x, y, iconSize, iconSize) || nearbyPoint.imageBitmap == null) {
					canvas.drawBitmap(cachedSmallIconBitmap, x, y, pointPaint);
					smallObjectsLatLon.add(new LatLon(lat, lon));
				} else {
					fullObjects.add(nearbyPoint);
					fullObjectsLatLon.add(new LatLon(lat, lon));
				}
			}
		}
		for (NearbyPlacePoint point : fullObjects) {
			Bitmap bitmap = point.imageBitmap;
			if (bitmap != null) {
				float x = tileBox.getPixXFromLatLon(point.getLatitude(), point.getLongitude());
				float y = tileBox.getPixYFromLatLon(point.getLatitude(), point.getLongitude());
				canvas.drawBitmap(PointImageUtils.createBigBitmap(getApplication(), bitmap), x, y, pointPaint);
			}
		}
	}

	public synchronized void showNearbyPoints() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		clearNearbyPoints();
		nearbyPlacesMapLayerProvider = new NearbyPlacesTileProvider(getApplication(),
				getPointsOrder(),
				view.getDensity());

		List<NearbyPlacePoint> points = customObjectsDelegate.getMapObjects();
		showNearbyPoints(points);
		nearbyPlacesMapLayerProvider.drawSymbols(mapRenderer);
	}

	private void showNearbyPoints(List<NearbyPlacePoint> points) {
		for (NearbyPlacePoint nearbyPlacePoint : points) {
			nearbyPlacesMapLayerProvider.addToData(nearbyPlacePoint);
		}
	}

	public void clearNearbyPoints() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || nearbyPlacesMapLayerProvider == null) {
			return;
		}
		nearbyPlacesMapLayerProvider.clearSymbols(mapRenderer);
		nearbyPlacesMapLayerProvider = null;
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof NearbyPlacePoint) {
			return ((NearbyPlacePoint) o).getPointDescription(getContext());
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof NearbyPlacePoint) {
			return new LatLon(((NearbyPlacePoint) o).getLatitude(), ((NearbyPlacePoint) o).getLongitude());
		}
		return null;
	}

	public void setCustomMapObjects(List<WikiCoreHelper.OsmandApiFeatureData> nearbyPlacePoints) {
		Picasso.get().cancelTag(LOAD_NEARBY_IMAGES_TAG);
		imageLoadingTargets.clear();
		List<NearbyPlacePoint> nearbyPlacePointsList = new ArrayList<>();
		for (WikiCoreHelper.OsmandApiFeatureData data : nearbyPlacePoints) {
			NearbyPlacePoint point = new NearbyPlacePoint(data);
			nearbyPlacePointsList.add(point);
			WikiImage wikiImage = WikiCoreHelper.getImageData(point.photoTitle);
			Target imgLoadTarget = new Target() {
				@Override
				public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
					point.imageBitmap = bitmap;
					customObjectsDelegate.onMapObjectUpdated(point);
					imageLoadingTargets.remove(this);
				}

				@Override
				public void onBitmapFailed(Exception e, Drawable errorDrawable) {
				}

				@Override
				public void onPrepareLoad(Drawable placeHolderDrawable) {
				}
			};
			imageLoadingTargets.add(imgLoadTarget);
			if (wikiImage != null) {
				Picasso.get()
						.load(wikiImage.getImageStubUrl())
						.tag(LOAD_NEARBY_IMAGES_TAG)
						.into(imgLoadTarget);
			}
		}
		customObjectsDelegate.setCustomMapObjects(nearbyPlacePointsList);
		getApplication().getOsmandMap().refreshMap();
	}
}