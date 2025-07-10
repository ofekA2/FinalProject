package com.example.finalproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalproject.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter(private var items: List<Review>) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    inner class VH(val binding: ItemReviewBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Review) {
            binding.tvRestaurant.text = item.restaurant
            binding.tvDetails.text = "${item.city} • ${item.cuisine}"
            binding.tvRating.text = "⭐ ${item.rating} / 10"
            binding.tvPriceTier.text = "$".repeat(item.priceTier)
            binding.tvReviewText.text = item.reviewText

            item.timestamp?.let { ts ->
                val df = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                binding.tvTimestamp.text = df.format(ts.toDate())
            }

            if (item.imageUrl.isNotBlank()) {
                Glide.with(binding.ivPhoto.context).load(item.imageUrl).into(binding.ivPhoto)
            }
            else {
                binding.ivPhoto.setImageDrawable(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    fun setData(newItems: List<Review>) {
        items = newItems
        notifyDataSetChanged()
    }
}