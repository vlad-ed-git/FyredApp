package com.dev_vlad.fyredapp.location

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer


class HotSpotClusterItemIconRenderer(
    private val hotSpotIcon: Bitmap,
    context: Context?,
    map: GoogleMap?,
    clusterManager: ClusterManager<HotSpotClusterItem>?
) : DefaultClusterRenderer<HotSpotClusterItem>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(
        item: HotSpotClusterItem,
        markerOptions: MarkerOptions
    ) {
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(hotSpotIcon))
        markerOptions.snippet(item.snippet)
        markerOptions.title(item.title)
        super.onBeforeClusterItemRendered(item, markerOptions)
    }


}