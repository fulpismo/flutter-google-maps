// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import io.flutter.plugin.common.MethodChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MarkersController {

  private final Map<String, MarkerController> markerIdToController;
  private final Map<String, String> googleMapsMarkerIdToDartMarkerId;
  private final MethodChannel methodChannel;
  private GoogleMap googleMap;
  private Bitmap defaultCountMarker;
  private final int size = 150;

  MarkersController(MethodChannel methodChannel) {
    this.markerIdToController = new HashMap<>();
    this.googleMapsMarkerIdToDartMarkerId = new HashMap<>();
    this.methodChannel = methodChannel;
    this.defaultCountMarker = createCountDefaultMarker();
  }

  private Bitmap createCountDefaultMarker() {
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    paint.setAntiAlias(true);
    Bitmap marker = Bitmap.createBitmap(size + 24, size + 24, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(marker);
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
    Rect rect = new Rect(0, 0, size, size);
    canvas.drawBitmap(marker, rect, rect, paint);
    int shadowWidth = size + 2;
    Paint shadow = new Paint();
    shadow.setColor(Color.BLACK);
    shadow.setStyle(Paint.Style.STROKE);
    shadow.setAlpha(25);
    shadow.setStrokeWidth(4);
    shadow.setAntiAlias(true);
    canvas.drawCircle(shadowWidth / 2f, shadowWidth / 2f, (shadowWidth / 2f) - 2f, shadow);
    return marker;
  }

  private Bitmap createPriceMarker(String text) {
    Paint textPaint = new Paint();
    textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    textPaint.setTextSize(size / 4f);
    textPaint.setAntiAlias(true);
    textPaint.setTextAlign(Paint.Align.LEFT);
    float textWidth = textPaint.measureText(text);
    Bitmap marker = Bitmap.createBitmap((size / 2) + (int)textWidth, size / 2, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(marker);
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    paint.setAntiAlias(true);
    canvas.drawRoundRect(new RectF(0, 0, (size / 2f)
            + textWidth, size / 2f), size / 1.5f, size / 1.5f, paint);
    Paint shadow = new Paint();
    int shadowWidth = size + 2;
    shadow.setColor(Color.BLACK);
    shadow.setStyle(Paint.Style.STROKE);
    shadow.setAlpha(25);
    shadow.setStrokeWidth(4);
    shadow.setAntiAlias(true);
    canvas.drawRoundRect(new RectF(0, 0, (shadowWidth / 2f)
            + textWidth, shadowWidth / 2f), shadowWidth / 1.5f, shadowWidth / 1.5f, shadow);

    Rect bounds = new Rect();
    paint.getTextBounds(text, 0, text.length(), bounds);
    float dx = size / 3f - bounds.width() / 3f - bounds.left;
    float dy = size / 3f + bounds.height() / 3f - bounds.bottom;

    canvas.drawText(text, dx, dy, textPaint);
    return marker;
  }

  private Bitmap addCountMarkerText(String text) {
    Bitmap marker = Bitmap.createBitmap(defaultCountMarker);
    Canvas canvas = new Canvas(marker);
    Paint paint = new Paint();
    Rect bounds = new Rect();

    paint.setColor(Color.BLACK);
    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    paint.setTextSize(size / 3f);
    paint.setAntiAlias(true);
    paint.setTextAlign(Paint.Align.LEFT);

    paint.getTextBounds(text, 0, text.length(), bounds);
    float dx = size / 2f - bounds.width() / 2f - bounds.left;
    float dy = size / 2f + bounds.height() / 2f - bounds.bottom;

    canvas.drawText(text, dx, dy, paint);
    return marker;
  }


  private Bitmap createBitmapFromMarker(Object marker) {
    final Map<?, ?> data = (Map<?, ?>) marker;
    if(data.get("count") != null) {
      return addCountMarkerText((String) data.get("count").toString());
    };
    if(data.get("price") != null) {
      return createPriceMarker((String) data.get("price"));
    };
    return null;
  }

  void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
  }

  void addMarkers(List<Object> markersToAdd) {
    if (markersToAdd != null) {
      for (Object markerToAdd : markersToAdd) {
        addMarker(markerToAdd);
      }
    }
  }

  void changeMarkers(List<Object> markersToChange) {
    if (markersToChange != null) {
      for (Object markerToChange : markersToChange) {
        changeMarker(markerToChange);
      }
    }
  }

  void removeMarkers(List<Object> markerIdsToRemove) {
    if (markerIdsToRemove == null) {
      return;
    }
    for (Object rawMarkerId : markerIdsToRemove) {
      if (rawMarkerId == null) {
        continue;
      }
      String markerId = (String) rawMarkerId;
      final MarkerController markerController = markerIdToController.remove(markerId);
      if (markerController != null) {
        markerController.remove();
        googleMapsMarkerIdToDartMarkerId.remove(markerController.getGoogleMapsMarkerId());
      }
    }
  }

  void showMarkerInfoWindow(String markerId, MethodChannel.Result result) {
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      markerController.showInfoWindow();
      result.success(null);
    } else {
      result.error("Invalid markerId", "showInfoWindow called with invalid markerId", null);
    }
  }

  void hideMarkerInfoWindow(String markerId, MethodChannel.Result result) {
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      markerController.hideInfoWindow();
      result.success(null);
    } else {
      result.error("Invalid markerId", "hideInfoWindow called with invalid markerId", null);
    }
  }

  void isInfoWindowShown(String markerId, MethodChannel.Result result) {
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      result.success(markerController.isInfoWindowShown());
    } else {
      result.error("Invalid markerId", "isInfoWindowShown called with invalid markerId", null);
    }
  }

  boolean onMarkerTap(String googleMarkerId) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return false;
    }
    methodChannel.invokeMethod("marker#onTap", Convert.markerIdToJson(markerId));
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      return markerController.consumeTapEvents();
    }
    return false;
  }

  void onMarkerDragStart(String googleMarkerId, LatLng latLng) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    final Map<String, Object> data = new HashMap<>();
    data.put("markerId", markerId);
    data.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("marker#onDragStart", data);
  }

  void onMarkerDrag(String googleMarkerId, LatLng latLng) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    final Map<String, Object> data = new HashMap<>();
    data.put("markerId", markerId);
    data.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("marker#onDrag", data);
  }

  void onMarkerDragEnd(String googleMarkerId, LatLng latLng) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    final Map<String, Object> data = new HashMap<>();
    data.put("markerId", markerId);
    data.put("position", Convert.latLngToJson(latLng));
    methodChannel.invokeMethod("marker#onDragEnd", data);
  }

  void onInfoWindowTap(String googleMarkerId) {
    String markerId = googleMapsMarkerIdToDartMarkerId.get(googleMarkerId);
    if (markerId == null) {
      return;
    }
    methodChannel.invokeMethod("infoWindow#onTap", Convert.markerIdToJson(markerId));
  }



  private void addMarker(Object marker) {
    if (marker == null) {
      return;
    }
    Bitmap bitmap = createBitmapFromMarker(marker);
    MarkerBuilder markerBuilder = new MarkerBuilder();
    String markerId = Convert.interpretMarkerOptions(marker, markerBuilder);
    MarkerOptions options = markerBuilder.build();
    options.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
    addMarker(markerId, options, markerBuilder.consumeTapEvents());
    bitmap.recycle();
  }

  private void addMarker(String markerId, MarkerOptions markerOptions, boolean consumeTapEvents) {
    final Marker marker = googleMap
        .addMarker(markerOptions);
    MarkerController controller = new MarkerController(marker, consumeTapEvents);
    markerIdToController.put(markerId, controller);
    googleMapsMarkerIdToDartMarkerId.put(marker.getId(), markerId);
  }

  private void changeMarker(Object marker) {
    if (marker == null) {
      return;
    }
    String markerId = getMarkerId(marker);
    MarkerController markerController = markerIdToController.get(markerId);
    if (markerController != null) {
      Convert.interpretMarkerOptions(marker, markerController);
    }
  }

  @SuppressWarnings("unchecked")
  private static String getMarkerId(Object marker) {
    Map<String, Object> markerMap = (Map<String, Object>) marker;
    return (String) markerMap.get("markerId");
  }
}
