package com.oceanhero.search.search.engine

import com.oceanhero.search.R

/**
 * The Ocean Hero search engine.
 *
 */
class OceanHeroSearch : BaseSearchEngine(
        "file:///android_asset/oceanhero.png",
        "https://oceanhero.today/web?q=",
        R.string.search_ocean_hero
)
