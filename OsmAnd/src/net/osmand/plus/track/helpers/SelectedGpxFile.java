package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectedGpxFile {

	public boolean notShowNavigationDialog;
	public boolean selectedByUser = true;

	protected GPXFile gpxFile;
	protected GPXTrackAnalysis trackAnalysis;

	protected Set<String> hiddenGroups = new HashSet<>();
	protected List<TrkSegment> processedPointsToDisplay = new ArrayList<>();
	protected QVectorPointI path31 = null;
	protected boolean path31FromGeneralTrack;
	protected List<GpxDisplayGroup> displayGroups;

	protected int color;
	protected long modifiedTime = -1;
	protected long pointsModifiedTime = -1;

	private boolean routePoints;
	protected boolean joinSegments;
	private boolean showCurrentTrack;
	protected boolean splitProcessed;

	private FilteredSelectedGpxFile filteredSelectedGpxFile;

	public void setGpxFile(GPXFile gpxFile, OsmandApplication app) {
		this.gpxFile = gpxFile;
		if (gpxFile.tracks.size() > 0) {
			this.color = gpxFile.tracks.get(0).getColor(0);
		}
		processPoints(app);
		if (filteredSelectedGpxFile != null) {
			app.getGpsFilterHelper().filterGpxFile(filteredSelectedGpxFile, false);
		}
	}

	public boolean isLoaded() {
		return gpxFile.modifiedTime != -1;
	}

	public GPXTrackAnalysis getTrackAnalysis(OsmandApplication app) {
		if (modifiedTime != gpxFile.modifiedTime) {
			update(app);
		}
		return trackAnalysis;
	}

	public GPXTrackAnalysis getTrackAnalysisToDisplay(OsmandApplication app) {
		return filteredSelectedGpxFile != null
				? filteredSelectedGpxFile.getTrackAnalysis(app)
				: getTrackAnalysis(app);
	}

	protected void update(@NonNull OsmandApplication app) {
		modifiedTime = gpxFile.modifiedTime;
		pointsModifiedTime = gpxFile.pointsModifiedTime;

		long fileTimestamp = Algorithms.isEmpty(gpxFile.path)
				? System.currentTimeMillis()
				: new File(gpxFile.path).lastModified();
		trackAnalysis = gpxFile.getAnalysis(fileTimestamp);

		displayGroups = null;
		splitProcessed = processSplit(app);

		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.update(app);
		}
	}

	protected boolean processSplit(@NonNull OsmandApplication app) {
		return GpxDisplayHelper.processSplit(app, this);
	}

	public void processPoints(OsmandApplication app) {
		update(app);
		this.processedPointsToDisplay = gpxFile.proccessPoints();
		if (this.processedPointsToDisplay.isEmpty()) {
			this.processedPointsToDisplay = gpxFile.processRoutePoints();
			routePoints = !this.processedPointsToDisplay.isEmpty();
		}
		if (app.getOsmandMap() != null &&
			app.getOsmandMap().getMapView() != null &&
			app.getOsmandMap().getMapView().hasMapRenderer()) {
			path31 = trackPointsToPath31(this.processedPointsToDisplay);
		} else {
			path31 = null;
		}
		path31FromGeneralTrack = false;
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.processPoints(app);
		}
	}

	public boolean isRoutePoints() {
		return routePoints;
	}

	@NonNull
	public List<TrkSegment> getPointsToDisplay() {
		if (filteredSelectedGpxFile != null) {
			return filteredSelectedGpxFile.getPointsToDisplay();
		} else if (joinSegments) {
			return gpxFile != null && gpxFile.getGeneralTrack() != null
					? gpxFile.getGeneralTrack().segments
					: Collections.emptyList();
		} else {
			return processedPointsToDisplay;
		}
	}

	@NonNull
	public QVectorPointI getPath31ToDisplay() {
		if (filteredSelectedGpxFile != null) {
			return filteredSelectedGpxFile.getPath31ToDisplay();
		} else if (joinSegments) {
			if (gpxFile == null) {
				return new QVectorPointI();
			}
			if (!gpxFile.hasGeneralTrack()) {
				if (gpxFile.getGeneralTrack() != null) {
					path31 = trackPointsToPath31(gpxFile.getGeneralTrack().segments);
					path31FromGeneralTrack = true;
					return path31;
				} else {					
					return new QVectorPointI();
				}
			} else {
				if (!path31FromGeneralTrack || path31 == null) {
					path31 = trackPointsToPath31(gpxFile.getGeneralTrack().segments);
					path31FromGeneralTrack = true;
				}
				return path31;
			}
		} else {
			if (path31 == null) {
				path31 = trackPointsToPath31(processedPointsToDisplay);
			}
			return path31;
		}
	}

	public QVectorPointI trackPointsToPath31(List<TrkSegment> segments) {
		QVectorPointI path = new QVectorPointI();
		for (TrkSegment segment : segments) {
			for (WptPt pt : segment.points) {
				int x = MapUtils.get31TileNumberX(pt.lon);
				int y = MapUtils.get31TileNumberY(pt.lat);
				path.add(new PointI(x, y));
			}
		}
		return path;
	}

	public List<TrkSegment> getModifiablePointsToDisplay() {
		return processedPointsToDisplay;
	}

	public Set<String> getHiddenGroups() {
		return Collections.unmodifiableSet(hiddenGroups);
	}

	public int getHiddenGroupsCount() {
		return hiddenGroups.size();
	}

	public void addHiddenGroups(@Nullable String group) {
		hiddenGroups.add(Algorithms.isBlank(group) ? null : group);
	}

	public void removeHiddenGroups(@Nullable String group) {
		hiddenGroups.remove(Algorithms.isBlank(group) ? null : group);
	}

	public boolean isGroupHidden(@Nullable String group) {
		return hiddenGroups.contains(Algorithms.isBlank(group) ? null : group);
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public GPXFile getGpxFileToDisplay() {
		return filteredSelectedGpxFile != null ? filteredSelectedGpxFile.getGpxFile() : gpxFile;
	}

	public GPXFile getModifiableGpxFile() {
		// call process points after
		return gpxFile;
	}

	public boolean isShowCurrentTrack() {
		return showCurrentTrack;
	}

	public void setShowCurrentTrack(boolean showCurrentTrack) {
		this.showCurrentTrack = showCurrentTrack;
	}

	public boolean isJoinSegments() {
		return joinSegments;
	}

	public void setJoinSegments(boolean joinSegments) {
		this.joinSegments = joinSegments;
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.setJoinSegments(joinSegments);
		}
	}

	public int getColor() {
		return color;
	}

	public long getModifiedTime() {
		return modifiedTime;
	}

	public long getPointsModifiedTime() {
		return pointsModifiedTime;
	}

	public void resetSplitProcessed() {
		splitProcessed = false;
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.splitProcessed = false;
		}
	}

	public List<GpxDisplayGroup> getDisplayGroups(@NonNull OsmandApplication app) {
		if (modifiedTime != gpxFile.modifiedTime || !splitProcessed) {
			update(app);
		}
		return filteredSelectedGpxFile != null ? filteredSelectedGpxFile.getDisplayGroups(app) : displayGroups;
	}

	public void setDisplayGroups(List<GpxDisplayGroup> displayGroups, OsmandApplication app) {
		if (filteredSelectedGpxFile != null) {
			filteredSelectedGpxFile.setDisplayGroups(displayGroups, app);
		} else {
			this.splitProcessed = true;
			this.displayGroups = displayGroups;

			if (modifiedTime != gpxFile.modifiedTime) {
				update(app);
			}
		}
	}

	@NonNull
	public FilteredSelectedGpxFile createFilteredSelectedGpxFile(@NonNull OsmandApplication app,
	                                                             @Nullable GpxDataItem gpxDataItem) {
		filteredSelectedGpxFile = new FilteredSelectedGpxFile(app, this, gpxDataItem);
		return filteredSelectedGpxFile;
	}

	@Nullable
	public FilteredSelectedGpxFile getFilteredSelectedGpxFile() {
		return filteredSelectedGpxFile;
	}
}
