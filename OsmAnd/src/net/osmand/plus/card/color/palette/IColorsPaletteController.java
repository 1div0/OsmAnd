package net.osmand.plus.card.color.palette;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.palette.data.PaletteColor;
import net.osmand.plus.card.color.palette.data.PaletteSortingMode;
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController.ColorPickerListener;

import java.util.List;

public interface IColorsPaletteController extends IDialogController, ColorPickerListener {

	String ALL_COLORS_PROCESS_ID = "show_all_colors_palette";

	void bindPalette(@NonNull IColorsPalette palette);

	void unbindPalette(@NonNull IColorsPalette palette);

	void setPaletteListener(@NonNull OnColorsPaletteListener onColorsPaletteListener);

	@ColorInt
	int getControlsAccentColor(boolean nightMode);

	boolean isAccentColorCanBeChanged();

	void onSelectColorFromPalette(@NonNull PaletteColor color);

	void onColorLongClick(@NonNull FragmentActivity activity, @NonNull View view, @NonNull PaletteColor color, boolean nightMode);

	void onAllColorsButtonClicked(@NonNull FragmentActivity activity);

	void onAddColorButtonClicked(@NonNull FragmentActivity activity);

	@NonNull
	List<PaletteColor> getColors(@NonNull PaletteSortingMode sortingMode);

	@Nullable
	PaletteColor getSelectedColor();

	boolean isSelectedColor(@NonNull PaletteColor paletteColor);

}
