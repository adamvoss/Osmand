package net.osmand.plus.download.ui;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.openseamapsplugin.NauticalMapsPlugin;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ItemViewHolder {

	private final java.text.DateFormat dateFormat;

	protected final TextView nameTextView;
	protected final TextView descrTextView;
	protected final ImageView leftImageView;
	protected final ImageView rightImageButton;
	protected final Button rightButton;
	protected final ProgressBar progressBar;

	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;
	private boolean nauticalPluginDisabled;
	private boolean freeVersion;
	
	protected final DownloadActivity context;
	
	private int textColorPrimary;
	private int textColorSecondary;
	private RightButtonAction clickAction;
	
	boolean showTypeInDesc;
	boolean showRemoteDate;

	

	private enum RightButtonAction {
		DOWNLOAD,
		ASK_FOR_SEAMARKS_PLUGIN,
		ASK_FOR_SRTM_PLUGIN_PURCHASE,
		ASK_FOR_SRTM_PLUGIN_ENABLE,
		ASK_FOR_FULL_VERSION_PURCHASE
	}
	

	public ItemViewHolder(View view, DownloadActivity context) {
		this.context = context;
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		rightButton = (Button) view.findViewById(R.id.rightButton);
		leftImageView = (ImageView) view.findViewById(R.id.leftImageView);
		descrTextView = (TextView) view.findViewById(R.id.description);
		rightImageButton = (ImageView) view.findViewById(R.id.rightImageButton);
		nameTextView = (TextView) view.findViewById(R.id.name);
		
		this.dateFormat = context.getMyApplication().getResourceManager().getDateFormat();

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		textColorPrimary = typedValue.data;
		theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
		textColorSecondary = typedValue.data;
	}
	
	public void setShowRemoteDate(boolean showRemoteDate) {
		this.showRemoteDate = showRemoteDate;
	}
	
	public void setShowTypeInDesc(boolean showTypeInDesc) {
		this.showTypeInDesc = showTypeInDesc;
	}


	// FIXME don't initialize on every row 
	private void initAppStatusVariables() {
		srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
		nauticalPluginDisabled = OsmandPlugin.getEnabledPlugin(NauticalMapsPlugin.class) == null;
		freeVersion = Version.isFreeVersion(context.getMyApplication());
		OsmandPlugin srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		srtmNeedsInstallation = srtmPlugin == null || srtmPlugin.needsInstallation();
	}

	public void bindIndexItem(final IndexItem indexItem) {
		initAppStatusVariables();
		boolean isDownloading = context.getDownloadThread().isDownloading(indexItem);
		int progress = -1;
		if (context.getDownloadThread().getCurrentDownloadingItem() == indexItem) {
			progress = context.getDownloadThread().getCurrentDownloadingItemProgress();
		}
		boolean disabled = checkDisabledAndClickAction(indexItem);
		/// name and left item
		nameTextView.setText(indexItem.getVisibleName(context, context.getMyApplication().getRegions(), false));
		if(!disabled) {
			nameTextView.setTextColor(textColorPrimary);
		} else {
			nameTextView.setTextColor(textColorSecondary);
		}
		int color = textColorSecondary;
		if(indexItem.isDownloaded()) {
			int colorId = indexItem.isOutdated() ? R.color.color_distance : R.color.color_ok;
			color = context.getResources().getColor(colorId);
		}
		if (indexItem.isDownloaded()) {
			leftImageView.setImageDrawable(getContentIcon(context,
					indexItem.getType().getIconResource(), color));
		} else if (disabled) {
			leftImageView.setImageDrawable(getContentIcon(context,
					indexItem.getType().getIconResource(), textColorSecondary));
		} else {
			leftImageView.setImageDrawable(getContentIcon(context,
					indexItem.getType().getIconResource()));
		}

		if (!isDownloading) {
			progressBar.setVisibility(View.GONE);
			
			descrTextView.setTextColor(color);
			descrTextView.setVisibility(View.VISIBLE);
			if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
					indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
				descrTextView.setText(indexItem.getType().getString(context));
			} else if (showTypeInDesc) {
				descrTextView.setText(indexItem.getType().getString(context) + 
						" • " + indexItem.getSizeDescription(context) +
						" • " + (showRemoteDate ? indexItem.getRemoteDate() : indexItem.getLocalDate()));
			} else {
				descrTextView.setText(indexItem.getSizeDescription(context) + " • " + (showRemoteDate ? indexItem.getRemoteDate() : indexItem.getLocalDate()));
			}
			
			rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_action_import));
			rightImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					context.startDownload(indexItem);
				}
			});
		} else {
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setProgress(progress);
			
			descrTextView.setVisibility(View.GONE);
			
			rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_action_remove_dark));
			rightImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					context.makeSureUserCancelDownload(indexItem);
				}
			});
		}
	}


	private boolean checkDisabledAndClickAction(final IndexItem indexItem) {
		boolean disabled = false;
		clickAction = RightButtonAction.DOWNLOAD;
		if (indexItem.getBasename().toLowerCase().equals(DownloadResources.WORLD_SEAMARKS_KEY)
				&& nauticalPluginDisabled) {
			clickAction = RightButtonAction.ASK_FOR_SEAMARKS_PLUGIN;
			disabled = true;
		} else if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE ||
				indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) && srtmDisabled) {
			if (srtmNeedsInstallation) {
				clickAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_PURCHASE;
			} else {
				clickAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_ENABLE;
			}

			disabled = true;
		} else if (indexItem.getType() == DownloadActivityType.WIKIPEDIA_FILE && freeVersion) {
			clickAction = RightButtonAction.ASK_FOR_FULL_VERSION_PURCHASE;
			disabled = true;
		}
		
		if (clickAction != RightButtonAction.DOWNLOAD) {
			rightButton.setText(R.string.get_plugin);
			rightButton.setVisibility(View.VISIBLE);
			rightImageButton.setVisibility(View.GONE);
			final RightButtonAction action = clickAction;

			rightButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (action) {
						case ASK_FOR_FULL_VERSION_PURCHASE:
							Intent intent = new Intent(Intent.ACTION_VIEW,
									Uri.parse(Version.marketPrefix(context.getMyApplication())
											+ "net.osmand.plus"));
							context.startActivity(intent);
							break;
						case ASK_FOR_SEAMARKS_PLUGIN:
							context.startActivity(new Intent(context,
									context.getMyApplication().getAppCustomization().getPluginsActivity()));
							AccessibleToast.makeText(context.getApplicationContext(),
									context.getString(R.string.activate_seamarks_plugin), Toast.LENGTH_SHORT).show();
							break;
						case ASK_FOR_SRTM_PLUGIN_PURCHASE:
							OsmandPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
							context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
							break;
						case ASK_FOR_SRTM_PLUGIN_ENABLE:
							context.startActivity(new Intent(context,
									context.getMyApplication().getAppCustomization().getPluginsActivity()));
							AccessibleToast.makeText(context,
									context.getString(R.string.activate_srtm_plugin), Toast.LENGTH_SHORT).show();
							break;
						case DOWNLOAD:
							break;
					}
				}
			});
		} else {
			rightButton.setVisibility(View.GONE);
			rightImageButton.setVisibility(View.VISIBLE);
		}
		
		return disabled;
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId) {
		return context.getMyApplication().getIconsCache().getContentIcon(resourceId);
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId, int color) {
		return context.getMyApplication().getIconsCache().getPaintedContentIcon(resourceId, color);
	}

	public boolean isItemAvailable() {
		return clickAction == RightButtonAction.DOWNLOAD;
	}
}
