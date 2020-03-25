package net.osmand.plus.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.PluginsActivity;
import net.osmand.plus.base.BaseOsmAndFragment;


import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UiCustomizationRootFragment extends BaseOsmAndFragment {

	public static final String TAG = UiCustomizationRootFragment.class.getName();
	private static final Log LOG = PlatformUtil.getLog(TAG);

	private OsmandApplication app;
	private LayoutInflater mInflater;
	private boolean nightMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		mInflater = UiUtilities.getInflater(app, nightMode);
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//			getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		ApplicationMode profile = app.getSettings().getApplicationMode();
		View root = mInflater.inflate(R.layout.fragment_ui_customization, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		TextView toolbarTitle = root.findViewById(R.id.toolbar_title);
		TextView toolbarSubTitle = root.findViewById(R.id.toolbar_subtitle);
		ImageButton toolbarButton = root.findViewById(R.id.close_button);
		RecyclerView recyclerView = root.findViewById(R.id.list);
		toolbar.setBackgroundColor(nightMode
				? getResources().getColor(R.color.list_background_color_dark)
				: getResources().getColor(R.color.list_background_color_light));
		toolbarTitle.setTextColor(nightMode
				? getResources().getColor(R.color.text_color_primary_dark)
				: getResources().getColor(R.color.list_background_color_dark));
		toolbarSubTitle.setTextColor(getResources().getColor(R.color.text_color_secondary_light));
		toolbarButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_arrow_back, getResources().getColor(R.color.text_color_secondary_light)));
		toolbarTitle.setText(R.string.ui_customization);
		toolbarSubTitle.setText(profile.toHumanString());
		toolbarSubTitle.setVisibility(View.VISIBLE);
		List<Object> items = new ArrayList<>();
		items.add(getString(R.string.ui_customization_description));
		items.addAll(Arrays.asList(ScreenType.values()));
		CustomizationItemsAdapter adapter = new CustomizationItemsAdapter(items, new OnCustomizationItemClickListener() {
			@Override
			public void onItemClick(ScreenType type) {
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					UiCustomizationFragment.showInstance(fm, type);
				}
			}
		});
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}
		return root;
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
	}


	private class CustomizationItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private static final int DESCRIPTION_TYPE = 0;
		private static final int ITEM_TYPE = 1;

		private List<Object> items;
		private OnCustomizationItemClickListener listener;

		CustomizationItemsAdapter(List<Object> items, OnCustomizationItemClickListener listener) {
			this.items = items;
			this.listener = listener;
		}

		@Override
		public int getItemViewType(int position) {
			if (items.get(position) instanceof String) {
				return DESCRIPTION_TYPE;
			} else {
				return ITEM_TYPE;
			}
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			if (viewType == DESCRIPTION_TYPE) {
				View view = mInflater.inflate(R.layout.list_item_description_with_image, parent, false);
				return new DescriptionHolder(view);

			} else {
				View view = mInflater.inflate(R.layout.list_item_ui_customization, parent, false);
				return new ItemHolder(view);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			final Object currentItem = items.get(position);
			if (holder instanceof DescriptionHolder) {
				DescriptionHolder descriptionHolder = (DescriptionHolder) holder;
				String plugins = getString(R.string.prefs_plugins) + '.';
				String description = String.format(getString(R.string.ltr_or_rtl_combine_via_space), (String) currentItem, plugins);
				setupClickableText(
						descriptionHolder.description, description, plugins, new Intent(app, PluginsActivity.class));
				descriptionHolder.image.setVisibility(View.GONE);
			} else {
				final ScreenType item = (ScreenType) currentItem;
				ItemHolder itemHolder = (ItemHolder) holder;
				itemHolder.icon.setImageResource(item.iconRes);
				itemHolder.title.setText(item.titleRes);
				itemHolder.itemView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						listener.onItemClick(item);
					}
				});
			}
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		private void setupClickableText(TextView textView, String text, String clickableText, final Intent intent) {
			SpannableString spannableString = new SpannableString(text);
			ClickableSpan clickableSpan = new ClickableSpan() {
				@Override
				public void onClick(@NonNull View view) {
					startActivity(intent);
				}
			};
			try {
				int startIndex = text.indexOf(clickableText);
				spannableString.setSpan(clickableSpan, startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				textView.setText(spannableString);
				textView.setMovementMethod(LinkMovementMethod.getInstance());
				textView.setHighlightColor(nightMode
						? getResources().getColor(R.color.active_color_primary_dark)
						: getResources().getColor(R.color.active_color_primary_light));
			} catch (RuntimeException e) {
				LOG.error("Error trying to find index of " + clickableText + " " + e);
			}
		}

		class DescriptionHolder extends RecyclerView.ViewHolder {
			ImageView image;
			TextView description;

			DescriptionHolder(@NonNull View itemView) {
				super(itemView);
				image = itemView.findViewById(R.id.image);
				description = itemView.findViewById(R.id.description);
			}
		}

		class ItemHolder extends RecyclerView.ViewHolder {
			ImageView icon;
			TextView title;
			TextView subTitle;

			ItemHolder(@NonNull View itemView) {
				super(itemView);
				icon = itemView.findViewById(R.id.icon);
				title = itemView.findViewById(R.id.title);
				subTitle = itemView.findViewById(R.id.sub_title);
			}
		}
	}

	public enum ScreenType {
		DRAWER(R.string.shared_string_drawer, R.drawable.ic_action_layers, R.drawable.img_settings_customize_drawer_day, R.drawable.img_settings_customize_drawer_night),
		CONFIGURE_MAP(R.string.configure_map, R.drawable.ic_action_layers, R.drawable.img_settings_customize_configure_map_day, R.drawable.img_settings_customize_configure_map_night),
		CONTEXT_MENU_ACTIONS(R.string.context_menu_actions, R.drawable.ic_action_layers, R.drawable.img_settings_customize_context_menu_day, R.drawable.img_settings_customize_context_menu_night);

		@StringRes
		public int titleRes;
		@DrawableRes
		public int iconRes;
		@DrawableRes
		public int imageDayRes;
		@DrawableRes
		public int imageNightRes;

		ScreenType(int titleRes, int iconRes, int imageDayRes, int imageNightRes) {
			this.titleRes = titleRes;
			this.iconRes = iconRes;
			this.imageDayRes = imageDayRes;
			this.imageNightRes = imageNightRes;
		}
	}

	interface OnCustomizationItemClickListener {
		void onItemClick(ScreenType type);
	}
}
