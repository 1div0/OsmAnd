package net.osmand.plus.base;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.util.TreeMap;

public class FavoriteImageDrawable extends Drawable {

	private boolean withShadow;
	private boolean synced;
	private boolean history;
	private Drawable favIcon;
	private Bitmap favBackgroundTop;
	private Bitmap favBackgroundCenter;
	private Bitmap favBackgroundBottom;
	private Bitmap syncedStroke;
	private Bitmap syncedColor;
	private Bitmap syncedShadow;
	private Bitmap syncedIcon;
	private Drawable uiListIcon;
	private Drawable uiBackgroundIcon;
	private Paint paintIcon = new Paint();
	private Paint paintBackground = new Paint();
	private ColorFilter colorFilter;
	private ColorFilter grayFilter;

	private FavoriteImageDrawable(Context ctx, int color, boolean withShadow, boolean synced, FavouritePoint point) {
		this.withShadow = withShadow;
		this.synced = synced;
		Resources res = ctx.getResources();
		int overlayIconId = point != null ? point.getOverlayIconId() : 0;
		int uiIconId;
		if (overlayIconId != 0) {
			favIcon = ((OsmandApplication) ctx.getApplicationContext()).getUIUtilities()
					.getIcon(getMapIconId(ctx, overlayIconId), R.color.color_white);
			uiIconId = overlayIconId;
		} else {
			favIcon = res.getDrawable(R.drawable.mm_special_star);
			uiIconId = R.drawable.mx_special_star;
		}
		int col = color == 0 || color == Color.BLACK ? res.getColor(R.color.color_favorite) : color;
		uiListIcon = ((OsmandApplication) ctx.getApplicationContext()).getUIUtilities()
				.getIcon(uiIconId, R.color.color_white);
		int uiBackgroundIconId = point != null ? point.getBackType().getIconId() : R.drawable.bg_point_circle;
		uiBackgroundIcon = ((OsmandApplication) ctx.getApplicationContext()).getUIUtilities()
				.getPaintedIcon(uiBackgroundIconId, col);
		int mapBackgroundIconIdTop = getMapBackIconId(ctx, point, "top");
		int mapBackgroundIconIdCenter = getMapBackIconId(ctx, point, "center");
		int mapBackgroundIconIdBottom = getMapBackIconId(ctx, point, "bottom");
		favBackgroundTop = BitmapFactory.decodeResource(res, mapBackgroundIconIdTop);
		favBackgroundCenter = BitmapFactory.decodeResource(res, mapBackgroundIconIdCenter);
		favBackgroundBottom = BitmapFactory.decodeResource(res, mapBackgroundIconIdBottom);
		syncedStroke = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_stroke);
		syncedColor = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_color);
		syncedShadow = BitmapFactory.decodeResource(res, R.drawable.map_shield_marker_point_shadow);
		syncedIcon = BitmapFactory.decodeResource(res, R.drawable.map_marker_point_14dp);
		colorFilter = new PorterDuffColorFilter(col, PorterDuff.Mode.SRC_IN);
		grayFilter = new PorterDuffColorFilter(res.getColor(R.color.color_favorite_gray), PorterDuff.Mode.MULTIPLY);
	}

	private int getMapIconId(Context ctx, int iconId) {
		String iconName = ctx.getResources().getResourceEntryName(iconId);
		return ctx.getResources().getIdentifier(iconName
				.replaceFirst("mx_", "mm_"), "drawable", ctx.getPackageName());
	}

	private int getMapBackIconId(Context ctx, FavouritePoint point, String layer) {
		if (point != null) {
			int iconId = point.getBackType().getIconId();
			String iconName = ctx.getResources().getResourceEntryName(iconId);
			return ctx.getResources().getIdentifier("map_" + iconName + "_" + layer
					, "drawable", ctx.getPackageName());
		}
		return R.drawable.map_white_favorite_shield;
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		Rect bs = new Rect(bounds);
		if (!withShadow && !synced) {
			uiBackgroundIcon.setBounds(bounds);
			bs.inset(bs.width() / 5, bs.height() / 5);
			uiListIcon.setBounds(bs);
		} else if (withShadow) {
			bs.inset(bs.width() / 3, bs.height() / 3);
			favIcon.setBounds(bs);
		}
	}

	@Override
	public int getIntrinsicHeight() {
		return synced ? syncedShadow.getHeight() : favBackgroundCenter.getHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return synced ? syncedShadow.getWidth() : favBackgroundCenter.getWidth();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		paintBackground.setColorFilter(history ? grayFilter : colorFilter);
		Rect bs = getBounds();
		if (synced) {
			drawBitmap(canvas, bs, syncedShadow, paintBackground);
			drawBitmap(canvas, bs, syncedColor, paintBackground);
			drawBitmap(canvas, bs, syncedStroke, paintBackground);
			drawBitmap(canvas, bs, syncedIcon, paintIcon);
		} else if (withShadow) {
			drawBitmap(canvas, bs, favBackgroundBottom, new Paint());
			drawBitmap(canvas, bs, favBackgroundCenter, paintBackground);
			drawBitmap(canvas, bs, favBackgroundTop, new Paint());
			favIcon.draw(canvas);
		} else {
			uiBackgroundIcon.draw(canvas);
			uiListIcon.draw(canvas);
		}
	}

	public void drawBitmap(@NonNull Canvas canvas, Rect bs, Bitmap syncedShadow, Paint paintBackground) {
		canvas.drawBitmap(syncedShadow, bs.exactCenterX() - syncedShadow.getWidth() / 2f,
				bs.exactCenterY() - syncedShadow.getHeight() / 2f, paintBackground);
	}

	public void drawBitmapInCenter(Canvas canvas, float x, float y, boolean history) {
		this.history = history;
		float dx = x - getIntrinsicWidth() / 2f;
		float dy = y - getIntrinsicHeight() / 2f;
		canvas.translate(dx, dy);
		draw(canvas);
		canvas.translate(-dx, -dy);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.UNKNOWN;
	}

	@Override
	public void setAlpha(int alpha) {
		paintBackground.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paintIcon.setColorFilter(cf);
	}

	private static TreeMap<String, FavoriteImageDrawable> cache = new TreeMap<>();

	private static FavoriteImageDrawable getOrCreate(Context ctx, int color, boolean withShadow, boolean synced, FavouritePoint point) {
		String uniqueId = "";
		if (point != null) {
			uniqueId = point.getIconEntryName(ctx);
			uniqueId += point.getBackType().name();
		}
		color = color | 0xff000000;
		int hash = (color << 4) + ((withShadow ? 1 : 0) << 2) + ((synced ? 3 : 0) << 2);
		uniqueId = hash + uniqueId;
		FavoriteImageDrawable drawable = cache.get(uniqueId);
		if (drawable == null) {
			drawable = new FavoriteImageDrawable(ctx, color, withShadow, synced, point);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(uniqueId, drawable);
		}
		return drawable;
	}

	public static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow, FavouritePoint point) {
		return getOrCreate(a, color, withShadow, false, point);
	}

	public static FavoriteImageDrawable getOrCreate(Context a, int color, boolean withShadow, GPXUtilities.WptPt pt) {
		return getOrCreate(a, color, withShadow, false, getFavouriteFromWpt(a, pt));
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context a, int color, FavouritePoint point) {
		return getOrCreate(a, color, false, true, point);
	}

	public static FavoriteImageDrawable getOrCreateSyncedIcon(Context a, int color, GPXUtilities.WptPt pt) {
		return getOrCreate(a, color, false, true, getFavouriteFromWpt(a, pt));
	}

	private static FavouritePoint getFavouriteFromWpt(Context a, GPXUtilities.WptPt pt) {
		FavouritePoint point = null;
		if (pt != null) {
			point = new FavouritePoint(pt.getLatitude(), pt.getLongitude(), pt.name, pt.category);
			point.setIconIdFromName(a, pt.getIconName());
		}
		return point;
	}
}
