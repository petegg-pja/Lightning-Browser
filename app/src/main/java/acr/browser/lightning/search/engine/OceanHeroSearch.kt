package acr.browser.lightning.search.engine

import acr.browser.lightning.R

/**
 * The Ocean Hero search engine.
 *
 */
class OceanHeroSearch : BaseSearchEngine(
        "file:///android_asset/oceanhero.png",
        "https://oceanhero.today/web?q=",
        R.string.search_ocean_hero
)
