package net.osmand.plus.mapmarkers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.adapters.MapMarkerDateViewHolder;
import net.osmand.plus.mapmarkers.adapters.MapMarkerItemViewHolder;
import net.osmand.plus.mapmarkers.adapters.MapMarkersHistoryAdapter;

public class MapMarkersHistoryFragment extends Fragment implements MapMarkersHelper.MapMarkerChangedListener {

	private MapMarkersHistoryAdapter adapter;
	private OsmandApplication app;
	private Paint backgroundPaint = new Paint();
	private Paint iconPaint = new Paint();
	private Paint textPaint = new Paint();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final boolean night = !app.getSettings().isLightContent();
		final MapActivity mapActivity = (MapActivity) getActivity();

		backgroundPaint.setColor(ContextCompat.getColor(getActivity(), night ? R.color.dashboard_divider_dark : R.color.dashboard_divider_light));
		backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		backgroundPaint.setAntiAlias(true);
		if (!night) {
			iconPaint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getActivity(), R.color.icon_color), PorterDuff.Mode.SRC_IN));
		}
		iconPaint.setAntiAlias(true);
		iconPaint.setFilterBitmap(true);
		iconPaint.setDither(true);
		textPaint.setTextSize(getResources().getDimension(R.dimen.default_desc_text_size));
		textPaint.setFakeBoldText(true);
		textPaint.setColor(ContextCompat.getColor(getActivity(), R.color.dashboard_subheader_text_light));
		textPaint.setAntiAlias(true);

		final String delStr = getString(R.string.shared_string_delete).toUpperCase();
		final String activateStr = getString(R.string.local_index_mi_restore).toUpperCase();
		Rect bounds = new Rect();

		textPaint.getTextBounds(activateStr, 0, activateStr.length(), bounds);
		final int activateStrWidth = bounds.width();
		final int textHeight = bounds.height();

		Fragment historyMarkerMenuFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(HistoryMarkerMenuBottomSheetDialogFragment.TAG);
		if (historyMarkerMenuFragment != null) {
			((HistoryMarkerMenuBottomSheetDialogFragment) historyMarkerMenuFragment).setListener(createHistoryMarkerMenuListener());
		}

		final RecyclerView recyclerView = new RecyclerView(getContext());
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			private float marginSides = getResources().getDimension(R.dimen.list_content_padding);
			private Bitmap deleteBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_delete_dark);
			private Bitmap resetBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_reset_to_default_dark);

			@Override
			public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
				if (viewHolder instanceof MapMarkerDateViewHolder) {
					return 0;
				}
				if (viewHolder instanceof MapMarkerItemViewHolder) {
					((MapMarkerItemViewHolder) viewHolder).setOptionsButtonVisibility(View.GONE);
				}
				return super.getSwipeDirs(recyclerView, viewHolder);
			}

			@Override
			public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
				return false;
			}

			@Override
			public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
				if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
					View itemView = viewHolder.itemView;
					float textMarginTop = ((float) itemView.getHeight() - (float) textHeight) / 2;
					if (dX > 0) {
						c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
						float iconMarginTop = ((float) itemView.getHeight() - (float) deleteBitmap.getHeight()) / 2;
						c.drawBitmap(deleteBitmap, itemView.getLeft() + marginSides, itemView.getTop() + iconMarginTop, iconPaint);
						c.drawText(delStr, itemView.getLeft() + 2 * marginSides + deleteBitmap.getWidth(), itemView.getTop() + textMarginTop + textHeight, textPaint);
					} else {
						c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), backgroundPaint);
						float iconMarginTop = ((float) itemView.getHeight() - (float) resetBitmap.getHeight()) / 2;
						c.drawBitmap(resetBitmap, itemView.getRight() - resetBitmap.getWidth() - marginSides, itemView.getTop() + iconMarginTop, iconPaint);
						c.drawText(activateStr, itemView.getRight() - resetBitmap.getWidth() - 2 * marginSides - activateStrWidth, itemView.getTop() + textMarginTop + textHeight, textPaint);
					}
				}
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}

			@Override
			public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
				if (viewHolder instanceof MapMarkerItemViewHolder) {
					((MapMarkerItemViewHolder) viewHolder).setOptionsButtonVisibility(View.VISIBLE);
				}
				super.clearView(recyclerView, viewHolder);
			}

			@Override
			public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
				int pos = viewHolder.getAdapterPosition();
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					if (direction == ItemTouchHelper.LEFT) {
						app.getMapMarkersHelper().restoreMarkerFromHistory((MapMarker) item, 0);
					} else {
						app.getMapMarkersHelper().removeMarkerFromHistory((MapMarker) item);
					}
					adapter.notifyItemRemoved(pos);
				}
			}
		};
		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
		itemTouchHelper.attachToRecyclerView(recyclerView);

		adapter = new MapMarkersHistoryAdapter(mapActivity.getMyApplication());
		adapter.setAdapterListener(new MapMarkersHistoryAdapter.MapMarkersHistoryAdapterListener() {
			@Override
			public void onItemClick(View view) {
				int pos = recyclerView.getChildAdapterPosition(view);
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					MapMarker marker = (MapMarker) item;
					HistoryMarkerMenuBottomSheetDialogFragment fragment = new HistoryMarkerMenuBottomSheetDialogFragment();
					Bundle arguments = new Bundle();
					arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_POSITION, pos);
					arguments.putString(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_NAME, marker.getName(mapActivity));
					arguments.putInt(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_COLOR_INDEX, marker.colorIndex);
					arguments.putLong(HistoryMarkerMenuBottomSheetDialogFragment.MARKER_VISITED_DATE, marker.visitedDate);
					fragment.setArguments(arguments);
					fragment.setListener(createHistoryMarkerMenuListener());
					fragment.show(mapActivity.getSupportFragmentManager(), HistoryMarkerMenuBottomSheetDialogFragment.TAG);
				}
			}
		});
		recyclerView.setAdapter(adapter);

		app.getMapMarkersHelper().addListener(this);

		return recyclerView;
	}

	private HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener createHistoryMarkerMenuListener() {
		return new HistoryMarkerMenuBottomSheetDialogFragment.HistoryMarkerMenuFragmentListener() {
			@Override
			public void onMakeMarkerActive(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					app.getMapMarkersHelper().restoreMarkerFromHistory((MapMarker) item, 0);
					adapter.notifyItemRemoved(pos);
				}
			}

			@Override
			public void onDeleteMarker(int pos) {
				Object item = adapter.getItem(pos);
				if (item instanceof MapMarker) {
					app.getMapMarkersHelper().removeMarkerFromHistory((MapMarker) item);
					adapter.notifyItemRemoved(pos);
				}
			}
		};
	}

	@Override
	public void onDestroy() {
		app.getMapMarkersHelper().removeListener(this);
		super.onDestroy();
	}

	void updateAdapter() {
		adapter.createHeaders();
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void onMapMarkerChanged(MapMarker mapMarker) {
		updateAdapter();
	}

	@Override
	public void onMapMarkersChanged() {
		updateAdapter();
	}
}
