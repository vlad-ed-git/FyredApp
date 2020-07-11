package com.dev_vlad.fyredapp.location

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class HotSpotClusterItem : ClusterItem {

    private val mPosition: LatLng
    private val mTitle: String
    private val mSnippet: String
    private val mTag: String

    constructor(lat: Double, lng: Double, tag: String) {
        mPosition = LatLng(lat, lng)
        mTitle = ""
        mSnippet = ""
        mTag = tag
    }

    constructor(lat: Double, lng: Double, title: String, snippet: String, tag: String) {
        mPosition = LatLng(lat, lng)
        mTitle = title
        mSnippet = snippet
        mTag = tag
    }

    override fun getPosition(): LatLng {
        return mPosition
    }

    override fun getTitle(): String {
        return mTitle
    }

    override fun getSnippet(): String {
        return mSnippet
    }

    fun getTag(): String {
        return mTag
    }
}
