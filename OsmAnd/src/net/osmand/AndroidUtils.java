package net.osmand;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import net.osmand.plus.R;

import java.util.Date;

public class AndroidUtils {

	/**
	 * @param context
	 * @return true if Hardware keyboard is available
	 */
	public static boolean isHardwareKeyboardAvailable(Context context) {
		return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}
	
	/**
	 * @param context
	 */
	public static void softKeyboardDelayed(final View view) {
		view.post(new Runnable() {
			@Override
			public void run() {
				if (!isHardwareKeyboardAvailable(view.getContext())) {
					InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
					if (imm != null) {
						imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
					}
				}
			}
		});
	}

	public static String formatDate(Context ctx, long time) {
		return DateFormat.getDateFormat(ctx).format(new Date(time));
	}
	
	public static String formatDateTime(Context ctx, long time) {
		Date d = new Date(time);
		return DateFormat.getDateFormat(ctx).format(d) +
				" " + DateFormat.getTimeFormat(ctx).format(d);
	}
	
	public static String formatTime(Context ctx, long time) {
		return DateFormat.getTimeFormat(ctx).format(new Date(time));
	}

	public static View findParentViewById(View view, int id) {
		ViewParent viewParent = view.getParent();

		while (viewParent != null && viewParent instanceof View) {
			View parentView = (View)viewParent;
			if (parentView.getId() == id)
				return parentView;

			viewParent = parentView.getParent();
		}

		return null;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static void setBackground(Context ctx, View view, boolean night, int lightResId, int darkResId) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			view.setBackground(ctx.getResources().getDrawable(night ? darkResId : lightResId,
					ctx.getTheme()));
		} else {
			view.setBackgroundDrawable(ctx.getResources().getDrawable(night ? darkResId : lightResId));
		}
	}

	public static void setDashButtonBackground(Context ctx, View view, boolean night) {
		setBackground(ctx, view, night, R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
	}

	public static void setTextPrimaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(night ?
				ctx.getResources().getColor(R.color.primary_text_dark)
				: ctx.getResources().getColor(R.color.primary_text_light));
	}

	public static void setTextSecondaryColor(Context ctx, TextView textView, boolean night) {
		textView.setTextColor(night ?
				ctx.getResources().getColor(R.color.secondary_text_dark)
				: ctx.getResources().getColor(R.color.secondary_text_light));
	}
}
