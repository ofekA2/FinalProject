package com.example.finalproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.databinding.ItemSimpleTextBinding

class SimpleStringAdapter(
    private val items: List<String>
) : RecyclerView.Adapter<SimpleStringAdapter.VH>() {

    inner class VH(val binding: ItemSimpleTextBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemSimpleTextBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.tvText.text = items[position]
    }
}