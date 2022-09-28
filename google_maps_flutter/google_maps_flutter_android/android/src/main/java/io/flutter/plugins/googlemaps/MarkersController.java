// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.googlemaps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

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
  private Bitmap defaultPriceMarker;
  private final int size = 150;

  MarkersController(MethodChannel methodChannel) {
    this.markerIdToController = new HashMap<>();
    this.googleMapsMarkerIdToDartMarkerId = new HashMap<>();
    this.methodChannel = methodChannel;
    createCountDefaultMarker();
    createPriceDefaultMarker();
  }

  private void createCountDefaultMarker() {
    Bitmap marker = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(marker);
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    paint.setAntiAlias(true);
    canvas.drawCircle(size / 2, size / 2, size / 2, paint);

    Rect rect = new Rect(0, 0, size, size);
    canvas.drawBitmap(marker, rect, rect, paint);
    defaultCountMarker = marker;
  }

  private void createPriceDefaultMarker() {
    Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(output);

    final Paint paint = new Paint();
    final Rect rect = new Rect(0, 0, size, size);
    final RectF rectF = new RectF(rect);
    final float roundPx = size;

    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    paint.setColor(Color.WHITE);
    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(output, rect, rect, paint);
    defaultPriceMarker = output;
  }

  private Bitmap addCountMarkerText(String text) {
      Bitmap marker = Bitmap.createBitmap(defaultCountMarker);
      Canvas canvas = new Canvas(marker);
      Paint paint = new Paint();
      paint.setColor(Color.BLACK);
      paint.setTextSize((float)(size / 3));
      paint.setAntiAlias(true);
      canvas.drawText(text, size / 2, size / 2, paint);
      return marker;
  }

  private Bitmap addPriceMarkerText(String text) {
    Bitmap marker = Bitmap.createBitmap(defaultPriceMarker);
    Canvas canvas = new Canvas(marker);
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.setTextSize((float)(size / 3));
    paint.setAntiAlias(true);
    canvas.drawText(text, size / 2, size / 2, paint);
    return marker;
  }


  private Bitmap createBitmapFromMarker(Object marker) {
    final Map<?, ?> data = (Map<?, ?>) marker;
    if(data.get("count") != null) {
      return addCountMarkerText((String) data.get("count").toString());
    };
    if(data.get("price") != null) {
      return addPriceMarkerText((String) data.get("price"));
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
