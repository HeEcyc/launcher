package com.wlisuha.applauncher.base

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.wlisuha.applauncher.BR
import java.util.*
import kotlin.collections.HashMap


abstract class BaseAdapter<T, V : ViewDataBinding> private constructor(initItems: List<T>? = null) :
    RecyclerView.Adapter<BaseAdapter.BaseItem<T, out ViewDataBinding>>() {
    protected var items: MutableList<T> = mutableListOf()

    init {
        initItems?.let { items.addAll(it) }
    }

    override fun onCreateViewHolder(@NonNull viewGroup: ViewGroup, i: Int): BaseItem<T, V> =
        createHolder(getViewBinding(LayoutInflater.from(viewGroup.context), viewGroup, i))

    abstract fun getViewBinding(inflater: LayoutInflater, viewGroup: ViewGroup, viewType: Int): V

    fun clearItems() {
        if (items.isEmpty()) return
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    open fun addItems(newItems: List<T>) {
        items.addAll(newItems)
    }

    open fun reloadData(items: List<T>) {
        onNewItems(items)
        notifyDataSetChanged()
    }

    fun onNewItems(items: List<T>) {
        this.items.clear()
        this.items.addAll(items)
    }

    fun getData() = items

    open fun updateItem(pos: Int) {
        if (pos == -1) return
        notifyItemChanged(pos, Unit)
    }

    open fun updateItem(t: T) {
        updateItem(items.indexOf(t))
    }

    fun removeItem(pos: Int) {
        if (pos == -1) return
        items.removeAt(pos)
        notifyItemRemoved(pos)
    }

    fun removeItem(item: T) {
        val pos = items.indexOf(item)
        if (pos == -1) return
        items.removeAt(pos)
        notifyItemRemoved(pos)
    }

    open fun createHolder(binding: V) = object : BaseItem<T, V>(binding) {}


    fun addItem(pos: Int, item: T) {
        items.add(pos, item)
        notifyItemInserted(pos)
    }

    fun addItem(item: T) {
        items.add(item)
        notifyItemInserted(items.size)
    }

    fun remove(onRemove: (T) -> Boolean) {
        getData().firstOrNull(onRemove)
            ?.let(::removeItem)
    }

    fun removeLastItem(): T? {
        val removedItem = items.removeLastOrNull()
        notifyItemRemoved(itemCount + 1)
        return removedItem
    }

    abstract class BaseItem<T, V : ViewDataBinding>(val binding: V) :
        RecyclerView.ViewHolder(binding.root) {

        open fun setVariable(t: T) {
            binding.setVariable(BR.item, t)
            binding.executePendingBindings()
        }

        open fun bind(t: T) {

        }

    }

    class Builder<T, V : ViewDataBinding> constructor(private val layoutId: Int? = null) {
        var onItemClick: ((item: T) -> Unit)? = null
        var onIndexedItemClick: ((item: T, postition: Int) -> Unit)? = null
        var onIndexedViewItemClick: ((view: V, item: T, postition: Int) -> Unit)? = null
        var initItems: List<T>? = null
        var itemViewTypeProvider: ((T) -> Int)? = null
        var viewBinding: ((inflater: LayoutInflater, viewGroup: ViewGroup, viewType: Int) -> V)? =
            null
        var onBind: ((item: T, binding: V, adapter: BaseAdapter<T, *>) -> Unit)? = null
        var viewsClick = HashMap<Int, ((item: T) -> Unit)>()
        var handleView: ((View) -> Unit)? = null
        var itemsSize: (() -> Int?)? = null
        var holderBind: ((BaseItem<T, out ViewDataBinding>) -> Unit)? = null

        private var itemProvider: ((Int) -> T)? = null

        private fun createViewBinding(inf: LayoutInflater, vg: ViewGroup): V =
            DataBindingUtil.bind(getView(inf, vg))!!

        private fun getView(inf: LayoutInflater, vg: ViewGroup) = inf
            .inflate(layoutId!!, vg, false)

        fun build() = object : BaseAdapter<T, ViewDataBinding>(initItems) {

            override fun getViewBinding(
                inflater: LayoutInflater,
                viewGroup: ViewGroup,
                viewType: Int
            ): V = viewBinding
                ?.invoke(inflater, viewGroup, viewType)
                ?: createViewBinding(inflater, viewGroup)

            override fun getItemCount() = (itemsSize?.invoke()
                ?: super.items.size)

            fun provideItem(position: Int) = itemProvider
                ?.invoke(position)
                ?: items[position]

            override fun onBindViewHolder(holder: BaseItem<T, out ViewDataBinding>, position: Int) {
                provideItem(position).let { item ->
                    holderBind?.invoke(holder)
                    holder.setVariable(item)
                    holder.binding.root.setOnClickListener {
                        onItemClick?.invoke(item)
                        onIndexedItemClick?.invoke(item, position)
                        onIndexedViewItemClick?.invoke(holder.binding as V, item, position)
                    }
                    holder.bind(item)
                    setViewsClick(holder, item)
                    setViewsHandler(holder.binding.root)
                    onBind?.invoke(item, holder.binding as V, this)
                }
            }

            private fun setViewsHandler(rootView: View) {
                handleView?.invoke(rootView)
            }

            private fun setViewsClick(holder: BaseItem<T, out ViewDataBinding>, item: T) {
                viewsClick.forEach { (key, value) ->
                    holder.binding.root
                        .findViewById<View?>(key)
                        ?.setOnClickListener { value.invoke(item) }
                }
            }

            override fun getItemViewType(position: Int) = itemViewTypeProvider
                ?.invoke(getData()[position])
                ?: super.getItemViewType(position)
        }
    }
}

inline fun <T, V : ViewDataBinding> createAdapter(
    layout: Int? = null,
    body: BaseAdapter.Builder<T, V>.() -> Unit
): BaseAdapter<T, ViewDataBinding> {
    val builder = BaseAdapter.Builder<T, V>(layout)
    builder.body()
    return builder.build()
}
