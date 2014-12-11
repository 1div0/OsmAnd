package net.osmand.plus.render;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.android.TileSourceProxyProvider;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.core.jni.PointI;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

public class MapVectorLayer extends BaseMapLayer {

	private OsmandMapTileView view;
	private ResourceManager resourceManager;
	private Paint paintImg;

	private RectF destImage = new RectF();
	private final MapTileLayer tileLayer;
	private boolean visible = false;
	private boolean oldRender = false;
	private String cachedUnderlay;
	private Integer cachedMapTransparency;
	private String cachedOverlay;
	private Integer cachedOverlayTransparency;

	public MapVectorLayer(MapTileLayer tileLayer, boolean oldRender) {
		this.tileLayer = tileLayer;
		this.oldRender = oldRender;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		resourceManager = view.getApplication().getResourceManager();
		paintImg = new Paint();
		paintImg.setFilterBitmap(true);
		paintImg.setAlpha(getAlpha());
	}

	public boolean isVectorDataVisible() {
		return visible && view.getZoom() >= view.getSettings().LEVEL_TO_SWITCH_VECTOR_RASTER.get();
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		if (!visible) {
			resourceManager.getRenderer().clearCache();
		}
	}

	@Override
	public int getMaximumShownMapZoom() {
		return 23;
	}

	@Override
	public int getMinimumShownMapZoom() {
		return 1;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {

	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {
		if (!visible) {
			return;
		}
		if (!isVectorDataVisible() && tileLayer != null) {
			tileLayer.drawTileMap(canvas, tilesRect);
			resourceManager.getRenderer().interruptLoadingMap();
		} else {
			final MapRendererView mapRenderer = view.getMapRenderer();
			if (mapRenderer != null && !oldRender) {
				NativeCoreContext.getMapRendererContext().setNightMode(drawSettings.isNightMode());
				OsmandSettings st = view.getApplication().getSettings();
				if (!Algorithms.objectEquals(st.MAP_UNDERLAY.get(), cachedUnderlay)) {
					cachedUnderlay = st.MAP_UNDERLAY.get();
					ITileSource tileSource = st.getTileSourceByName(cachedUnderlay, false);
					if (tileSource != null) {
						TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), tileSource);
						mapRenderer.setMapLayerProvider(-1, prov.instantiateProxy(true));
						prov.swigReleaseOwnership();
					} else {
						mapRenderer.resetMapLayerProvider(-1);
					}
				}
				if (st.MAP_TRANSPARENCY.get() != cachedMapTransparency) {
					cachedMapTransparency = st.MAP_TRANSPARENCY.get();
					MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
					mapLayerConfiguration.setOpacity(((float)cachedMapTransparency) / 255.0f);
					mapRenderer.setMapLayerConfiguration(0, mapLayerConfiguration);
				}
				if (!Algorithms.objectEquals(st.MAP_OVERLAY.get(), cachedOverlay)) {
					cachedOverlay = st.MAP_OVERLAY.get();
					ITileSource tileSource = st.getTileSourceByName(cachedOverlay, false);
					if (tileSource != null) {
						TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), tileSource);
						mapRenderer.setMapLayerProvider(1, prov.instantiateProxy(true));
						prov.swigReleaseOwnership();
					} else {
						mapRenderer.resetMapLayerProvider(1);
					}
				}
				if (st.MAP_OVERLAY_TRANSPARENCY.get() != cachedOverlayTransparency) {
					cachedOverlayTransparency = st.MAP_OVERLAY_TRANSPARENCY.get();
					MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
					mapLayerConfiguration.setOpacity(((float)cachedOverlayTransparency) / 255.0f);
					mapRenderer.setMapLayerConfiguration(1, mapLayerConfiguration);
				}
				// opengl renderer
				mapRenderer.setTarget(new PointI(tilesRect.getCenter31X(), tilesRect.getCenter31Y()));
				mapRenderer.setAzimuth(-tilesRect.getRotate());
				mapRenderer.setZoom((float) (tilesRect.getZoom() /* + tilesRect.getZoomScale() */+ tilesRect
						.getZoomAnimation()));
			} else {
				if (!view.isZooming()) {
					if (resourceManager.updateRenderedMapNeeded(tilesRect, drawSettings)) {
						// pixRect.set(-view.getWidth(), -view.getHeight() / 2, 2 * view.getWidth(), 3 *
						// view.getHeight() / 2);
						final RotatedTileBox copy = tilesRect.copy();
						copy.increasePixelDimensions(copy.getPixWidth() / 3, copy.getPixHeight() / 4);
						resourceManager.updateRendererMap(copy);
					}

				}

				MapRenderRepositories renderer = resourceManager.getRenderer();
				drawRenderedMap(canvas, renderer.getBitmap(), renderer.getBitmapLocation(), tilesRect);
				drawRenderedMap(canvas, renderer.getPrevBitmap(), renderer.getPrevBmpLocation(), tilesRect);
			}
		}
	}

	private boolean drawRenderedMap(Canvas canvas, Bitmap bmp, RotatedTileBox bmpLoc, RotatedTileBox currentViewport) {
		boolean shown = false;
		if (bmp != null && bmpLoc != null) {
			float rot = -bmpLoc.getRotate();
			int cz = currentViewport.getZoom();
			canvas.rotate(rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
			final RotatedTileBox calc = currentViewport.copy();
			calc.setRotate(bmpLoc.getRotate());
			QuadPointDouble lt = bmpLoc.getLeftTopTile(cz);
			QuadPointDouble rb = bmpLoc.getRightBottomTile(cz);
			final float x1 = calc.getPixXFromTile(lt.x, lt.y, cz);
			final float x2 = calc.getPixXFromTile(rb.x, rb.y, cz);
			final float y1 = calc.getPixYFromTile(lt.x, lt.y, cz);
			final float y2 = calc.getPixYFromTile(rb.x, rb.y, cz);
			destImage.set(x1, y1, x2, y2);
			if (!bmp.isRecycled()) {
				canvas.drawBitmap(bmp, null, destImage, paintImg);
				shown = true;
			}
			canvas.rotate(-rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
		}
		return shown;
	}

	@Override
	public void setAlpha(int alpha) {
		super.setAlpha(alpha);
		if (paintImg != null) {
			paintImg.setAlpha(alpha);
		}
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}
}
