package com.oceanhero.search.browser.fragment

import com.oceanhero.search.R
import com.oceanhero.search.adblock.allowlist.AllowListModel
import com.oceanhero.search.animation.AnimationUtils
import com.oceanhero.search.browser.BookmarksView
import com.oceanhero.search.browser.TabsManager
import com.oceanhero.search.browser.bookmark.BookmarkUiModel
import com.oceanhero.search.constant.LOAD_READING_URL
import com.oceanhero.search.controller.UIController
import com.oceanhero.search.database.Bookmark
import com.oceanhero.search.database.bookmark.BookmarkRepository
import com.oceanhero.search.di.DatabaseScheduler
import com.oceanhero.search.di.MainScheduler
import com.oceanhero.search.di.NetworkScheduler
import com.oceanhero.search.di.injector
import com.oceanhero.search.dialog.BrowserDialog
import com.oceanhero.search.dialog.DialogItem
import com.oceanhero.search.dialog.LightningDialogBuilder
import com.oceanhero.search.extensions.color
import com.oceanhero.search.extensions.drawable
import com.oceanhero.search.favicon.FaviconModel
import com.oceanhero.search.reading.activity.ReadingActivity
import com.oceanhero.search.utils.isSpecialUrl
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.bookmark_drawer.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class BookmarksFragment : Fragment(), View.OnClickListener, View.OnLongClickListener, BookmarksView {

    // Managers
    @Inject internal lateinit var bookmarkModel: BookmarkRepository

    // Dialog builder
    @Inject internal lateinit var bookmarksDialogBuilder: LightningDialogBuilder

    @Inject internal lateinit var faviconModel: FaviconModel

    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:NetworkScheduler internal lateinit var networkScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler
    @Inject internal lateinit var whitelistModel: AllowListModel

    private lateinit var uiController: UIController

    // Adapter
    private var bookmarkAdapter: BookmarkListAdapter? = null

    // Colors
    private var scrollIndex: Int = 0

    private var isIncognito: Boolean = false

    private var bookmarksSubscription: Disposable? = null
    private var bookmarkUpdateSubscription: Disposable? = null

    private val uiModel = BookmarkUiModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        val context = requireNotNull(context) { "Context should never be null in onCreate" }

        uiController = context as UIController
        isIncognito = arguments?.getBoolean(INCOGNITO_MODE, false) == true
    }

    override fun onResume() {
        super.onResume()
        if (bookmarkAdapter != null) {
            setBookmarksShown(null, false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.bookmark_drawer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.bookmark_back_button).setOnClickListener {
            if (!uiModel.isCurrentFolderRoot()) {
                setBookmarksShown(null, true)
                bookmark_list_view?.layoutManager?.scrollToPosition(scrollIndex)
            }
        }
        setupNavigationButton(view, R.id.action_add_bookmark)
        setupNavigationButton(view, R.id.action_reading)
        setupNavigationButton(view, R.id.action_page_tools)


        bookmarkAdapter = BookmarkListAdapter(
            view.context,
            faviconModel,
            networkScheduler,
            mainScheduler,
            this::handleItemLongPress,
            this::handleItemClick
        )

        bookmark_list_view?.let {
            it.layoutManager = LinearLayoutManager(context)
            it.adapter = bookmarkAdapter
        }

        setBookmarksShown(null, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        bookmarksSubscription?.dispose()
        bookmarkUpdateSubscription?.dispose()

        bookmarkAdapter?.cleanupSubscriptions()
    }

    override fun onDestroy() {
        super.onDestroy()

        bookmarksSubscription?.dispose()
        bookmarkUpdateSubscription?.dispose()

        bookmarkAdapter?.cleanupSubscriptions()
    }

    private fun getTabsManager(): TabsManager = uiController.getTabModel()

    private fun updateBookmarkIndicator(url: String) {
        bookmarkUpdateSubscription?.dispose()
        bookmarkUpdateSubscription = bookmarkModel.isBookmark(url)
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { isBookmark ->
                bookmarkUpdateSubscription = null
                val activity = activity
                if (action_add_bookmark_image == null || activity == null) {
                    return@subscribe
                }
                action_add_bookmark_image.isSelected = isBookmark
                action_add_bookmark.isEnabled = !url.isSpecialUrl()
                action_add_bookmark_image.isEnabled = !url.isSpecialUrl()
            }
    }

    override fun handleBookmarkDeleted(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> setBookmarksShown(null, false)
        is Bookmark.Entry -> bookmarkAdapter?.deleteItem(BookmarkViewModel(bookmark)) ?: Unit
    }

    private fun setBookmarksShown(folder: String?, animate: Boolean) {
        bookmarksSubscription?.dispose()
        bookmarksSubscription = bookmarkModel.getBookmarksFromFolderSorted(folder)
            .concatWith(Single.defer {
                if (folder == null) {
                    bookmarkModel.getFoldersSorted()
                } else {
                    Single.just(emptyList())
                }
            })
            .toList()
            .map { it.flatten() }
            .subscribeOn(databaseScheduler)
            .observeOn(mainScheduler)
            .subscribe { bookmarksAndFolders ->
                uiModel.currentFolder = folder
                setBookmarkDataSet(bookmarksAndFolders, animate)
            }
    }

    private fun setBookmarkDataSet(items: List<Bookmark>, animate: Boolean) {
        bookmarkAdapter?.updateItems(items.map { BookmarkViewModel(it) })
        val resource = if (uiModel.isCurrentFolderRoot()) {
            R.drawable.ic_action_star
        } else {
            R.drawable.ic_action_back
        }

        if (animate) {
            bookmark_back_button_image?.let {
                val transition = AnimationUtils.createRotationTransitionAnimation(it, resource)
                it.startAnimation(transition)
            }
        } else {
            bookmark_back_button_image?.setImageResource(resource)
        }
    }

    private fun setupNavigationButton(view: View, @IdRes buttonId: Int) {
        val frameButton = view.findViewById<FrameLayout>(buttonId)
        frameButton.setOnClickListener(this)
        frameButton.setOnLongClickListener(this)
    }

    private fun handleItemLongPress(bookmark: Bookmark): Boolean {
        (context as Activity?)?.let {
            when (bookmark) {
                is Bookmark.Folder -> bookmarksDialogBuilder.showBookmarkFolderLongPressedDialog(it, uiController, bookmark)
                is Bookmark.Entry -> bookmarksDialogBuilder.showLongPressedDialogForBookmarkUrl(it, uiController, bookmark)
            }
        }
        return true
    }

    private fun handleItemClick(bookmark: Bookmark) = when (bookmark) {
        is Bookmark.Folder -> {
            scrollIndex = (bookmark_list_view.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            setBookmarksShown(bookmark.title, true)
        }
        is Bookmark.Entry -> uiController.bookmarkItemClicked(bookmark)
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.action_add_bookmark -> uiController.bookmarkButtonClicked()
            R.id.action_reading -> {
                val currentTab = getTabsManager().currentTab
                if (currentTab != null) {
                    val read = Intent(activity, ReadingActivity::class.java)
                    read.putExtra(LOAD_READING_URL, currentTab.url)
                    startActivity(read)
                }
            }
            R.id.action_page_tools -> {
                activity?.also(this::showPageToolsDialog)
            }
            else -> Unit
        }
    }

    /**
     * Show the page tools dialog.
     */
    private fun showPageToolsDialog(activity: Activity) {
        val currentTab = getTabsManager().currentTab ?: return
        val isAllowedAds = whitelistModel.isUrlAllowedAds(currentTab.url)
        val whitelistString = if (isAllowedAds) {
            R.string.dialog_adblock_enable_for_site
        } else {
            R.string.dialog_adblock_disable_for_site
        }

        BrowserDialog.showWithIcons(activity, activity.getString(R.string.dialog_tools_title),
            DialogItem(
                icon = activity.drawable(R.drawable.ic_action_desktop),
                title = R.string.dialog_toggle_desktop
            ) {
                getTabsManager().currentTab?.apply {
                    toggleDesktopUA()
                    reload()
                    // TODO add back drawer closing
                }
            },
            DialogItem(
                icon = activity.drawable(R.drawable.ic_block),
                colorTint = activity.color(R.color.error_red).takeIf { isAllowedAds },
                title = whitelistString,
                isConditionMet = !currentTab.url.isSpecialUrl()
            ) {
                if (isAllowedAds) {
                    whitelistModel.removeUrlFromAllowList(currentTab.url)
                } else {
                    whitelistModel.addUrlToAllowList(currentTab.url)
                }
                getTabsManager().currentTab?.reload()
            }
        )
    }

    override fun onLongClick(v: View) = false

    override fun navigateBack() {
        if (uiModel.isCurrentFolderRoot()) {
            uiController.onBackButtonPressed()
        } else {
            setBookmarksShown(null, true)
            bookmark_list_view?.layoutManager?.scrollToPosition(scrollIndex)
        }
    }

    override fun handleUpdatedUrl(url: String) {
        updateBookmarkIndicator(url)
        val folder = uiModel.currentFolder
        setBookmarksShown(folder, false)
    }

    private class BookmarkViewHolder(
        itemView: View,
        private val adapter: BookmarkListAdapter,
        private val onItemLongClickListener: (Bookmark) -> Boolean,
        private val onItemClickListener: (Bookmark) -> Unit
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        var txtTitle: TextView = itemView.findViewById(R.id.textBookmark)
        var favicon: ImageView = itemView.findViewById(R.id.faviconBookmark)

        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val index = adapterPosition
            if (index.toLong() != RecyclerView.NO_ID) {
                onItemClickListener(adapter.itemAt(index).bookmark)
            }
        }

        override fun onLongClick(v: View): Boolean {
            val index = adapterPosition
            return index != RecyclerView.NO_POSITION && onItemLongClickListener(adapter.itemAt(index).bookmark)
        }
    }

    private class BookmarkListAdapter(
        context: Context,
        private val faviconModel: FaviconModel,
        private val networkScheduler: Scheduler,
        private val mainScheduler: Scheduler,
        private val onItemLongClickListener: (Bookmark) -> Boolean,
        private val onItemClickListener: (Bookmark) -> Unit
    ) : RecyclerView.Adapter<BookmarkViewHolder>() {

        private var bookmarks: List<BookmarkViewModel> = listOf()
        private val faviconFetchSubscriptions = ConcurrentHashMap<String, Disposable>()
        private val folderIcon = context.drawable(R.drawable.ic_folder)
        private val webpageIcon = context.drawable(R.drawable.ic_webpage)

        fun itemAt(position: Int): BookmarkViewModel = bookmarks[position]

        fun deleteItem(item: BookmarkViewModel) {
            val newList = bookmarks - item
            updateItems(newList)
        }

        fun updateItems(newList: List<BookmarkViewModel>) {
            val oldList = bookmarks
            bookmarks = newList

            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size

                override fun getNewListSize() = bookmarks.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition] == bookmarks[newItemPosition]

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldList[oldItemPosition] == bookmarks[newItemPosition]
            })

            diffResult.dispatchUpdatesTo(this)
        }

        fun cleanupSubscriptions() {
            for (subscription in faviconFetchSubscriptions.values) {
                subscription.dispose()
            }
            faviconFetchSubscriptions.clear()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemView = inflater.inflate(R.layout.bookmark_list_item, parent, false)

            return BookmarkViewHolder(itemView, this, onItemLongClickListener, onItemClickListener)
        }

        override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
            holder.itemView.jumpDrawablesToCurrentState()

            val viewModel = bookmarks[position]
            holder.txtTitle.text = viewModel.bookmark.title

            val url = viewModel.bookmark.url
            holder.favicon.tag = url

            viewModel.icon?.let {
                holder.favicon.setImageBitmap(it)
                return
            }

            val imageDrawable = when (viewModel.bookmark) {
                is Bookmark.Folder -> folderIcon
                is Bookmark.Entry -> webpageIcon.also {
                    faviconFetchSubscriptions[url]?.dispose()
                    faviconFetchSubscriptions[url] = faviconModel
                        .faviconForUrl(url, viewModel.bookmark.title)
                        .subscribeOn(networkScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                            onSuccess = { bitmap ->
                                viewModel.icon = bitmap
                                if (holder.favicon.tag == url) {
                                    holder.favicon.setImageBitmap(bitmap)
                                }
                            }
                        )
                }
            }

            holder.favicon.setImageDrawable(imageDrawable)
        }

        override fun getItemCount() = bookmarks.size
    }

    companion object {

        @JvmStatic
        fun createFragment(isIncognito: Boolean) = BookmarksFragment().apply {
            arguments = Bundle().apply {
                putBoolean(INCOGNITO_MODE, isIncognito)
            }
        }

        private const val TAG = "BookmarksFragment"

        private const val INCOGNITO_MODE = "$TAG.INCOGNITO_MODE"
    }

}

private class BookmarkViewModel(
    val bookmark: Bookmark,
    var icon: Bitmap? = null
) {
    override fun equals(other: Any?): Boolean {
        return if (other is BookmarkViewModel) {
            bookmark == other.bookmark
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int = bookmark.hashCode()

    override fun toString(): String = "BookmarkViewModel(bookmark=$bookmark)"

}
