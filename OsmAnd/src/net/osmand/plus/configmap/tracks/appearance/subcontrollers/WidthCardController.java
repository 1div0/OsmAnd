package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.util.Algorithms.parseIntSilently;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.base.slider.moded.ModedSliderCard;
import net.osmand.plus.card.color.ISelectedColorProvider;
import net.osmand.plus.card.width.WidthComponentController;
import net.osmand.plus.card.width.WidthMode;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.track.fragments.TrackAppearanceFragment.OnNeedScrollListener;
import net.osmand.plus.track.fragments.controller.TrackWidthController.OnTrackWidthSelectedListener;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class WidthCardController extends BaseMultiStateCardController {

	private static final int UNCHANGED_STYLE_CARD_ID = 0;
	private static final int WIDTH_COMPONENT_CARD_ID = 1;

	private static final int CUSTOM_WIDTH_MIN = 1;
	private static final int CUSTOM_WIDTH_MAX = 24;

	private final AppearanceData appearanceData;

	private ISelectedColorProvider colorProvider;
	private WidthComponentController widthComponentController;
	private OnNeedScrollListener onNeedScrollListener;
	private OnTrackWidthSelectedListener listener;

	public WidthCardController(@NonNull OsmandApplication app, @NonNull AppearanceData appearanceData) {
		super(app, appearanceData.getWidthValue());
		this.appearanceData = appearanceData;
	}

	public void setListener(@NonNull OnTrackWidthSelectedListener listener) {
		this.listener = listener;
	}

	public void setOnNeedScrollListener(@NonNull OnNeedScrollListener onNeedScrollListener) {
		this.onNeedScrollListener = onNeedScrollListener;
	}

	public void setColorProvider(@NonNull ISelectedColorProvider colorProvider) {
		this.colorProvider = colorProvider;
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.shared_string_width);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		return selectedCardState.getTag() == null
				? selectedCardState.toHumanString(app)
				: getWidthComponentController().getSummary(app);
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		String widthValue = getWidthValue(cardState);
		onWidthValueSelected(widthValue);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		if (selectedCardState.getTag() == null) {
			bindSummaryCard(activity, container, nightMode);
		} else {
			bindWidthComponentCardIfNeeded(activity, container);
		}
	}

	private void bindSummaryCard(@NonNull FragmentActivity activity,
	                             @NonNull ViewGroup container, boolean nightMode) {
		container.removeAllViews();
		LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
		inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);

		String summary = app.getString(R.string.unchanged_parameter_summary);
		DescriptionCard descriptionCard = new DescriptionCard(activity, summary);
		container.addView(descriptionCard.build(activity));
		container.setTag(UNCHANGED_STYLE_CARD_ID);
	}

	private void bindWidthComponentCardIfNeeded(@NonNull FragmentActivity activity,
	                                            @NonNull ViewGroup container) {
		WidthComponentController controller = getWidthComponentController();
		controller.setOnNeedScrollListener(onNeedScrollListener);
		// We only create and bind "Width Component" card only if it wasn't attached before
		// or if there is other card visible at the moment.
		Integer cardId = (Integer) container.getTag();
		if (cardId == null || cardId == UNCHANGED_STYLE_CARD_ID) {
			container.removeAllViews();
			ModedSliderCard widthComponentCard = new ModedSliderCard(activity, controller);
			container.addView(widthComponentCard.build(activity));
			updateColorItems();
		}
		controller.askSelectWidthMode(getWidthValue(selectedCardState));
		container.setTag(WIDTH_COMPONENT_CARD_ID);
	}

	private void onWidthValueSelected(@Nullable String widthValue) {
		selectedCardState = findCardStateByWidthValue(widthValue);
		cardInstance.updateSelectedCardState();
		appearanceData.setWidthValue(widthValue);
		listener.onTrackWidthSelected(widthValue);
	}

	public void updateColorItems() {
		int currentColor = colorProvider.getSelectedColorValue();
		WidthComponentController controller = getWidthComponentController();
		controller.updateColorItems(currentColor);
	}

	@NonNull
	private WidthComponentController getWidthComponentController() {
		if (widthComponentController == null) {
			String selectedWidth = appearanceData.getWidthValue();
			WidthMode widthMode = WidthMode.valueOfKey(selectedWidth);
			int customValue = parseIntSilently(selectedWidth, CUSTOM_WIDTH_MIN);
			widthComponentController = new WidthComponentController(widthMode, customValue, this::onWidthValueSelected) {
				@NonNull
				@Override
				public Limits getSliderLimits() {
					return new Limits(CUSTOM_WIDTH_MIN, CUSTOM_WIDTH_MAX);
				}
			};
		}
		return widthComponentController;
	}

	@NonNull
	@Override
	protected CardState findCardState(@Nullable Object tag) {
		if (tag instanceof String) {
			return findCardStateByWidthValue((String) tag);
		}
		return super.findCardState(tag);
	}

	@NonNull
	private CardState findCardStateByWidthValue(@Nullable String width) {
		return findCardState(width != null ? WidthMode.valueOfKey(width) : null);
	}

	@Nullable
	private String getWidthValue(@NonNull CardState cardState) {
		if (isCustomValue(cardState)) {
			WidthComponentController controller = getWidthComponentController();
			return controller.getSelectedCustomValue();
		}
		if (cardState.getTag() instanceof WidthMode) {
			return ((WidthMode) cardState.getTag()).getKey();
		}
		return null;
	}

	private boolean isCustomValue(@NonNull CardState cardState) {
		return cardState.getTag() == WidthMode.CUSTOM;
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> result = new ArrayList<>();
		result.add(new CardState(R.string.shared_string_unchanged));
		for (WidthMode widthMode : WidthMode.values()) {
			result.add(new CardState(widthMode.getTitleId())
					.setShowTopDivider(widthMode.ordinal() == 0)
					.setTag(widthMode)
			);
		}
		return result;
	}
}
