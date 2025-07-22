package com.example.finalproject

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalproject.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class ReviewAdapter(private var items: List<Review>) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    inner class VH(val binding: ItemReviewBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Review) {
            binding.tvRestaurant.text = item.restaurant
            binding.tvDetails.text = "${item.city} • ${item.cuisine}"
            val r = ((item.rating?:0.0)*10).roundToInt()/10.0
            binding.tvRating.text = "⭐ %.1f/10".format(r)
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

            if (item.authorPhoto.isNotBlank()) {
                Glide.with(binding.ivAuthorPhoto.context).load(item.authorPhoto).circleCrop().into(binding.ivAuthorPhoto)
            }
            else {
                binding.ivAuthorPhoto.setImageResource(R.drawable.ic_profile)
            }

            binding.tvAuthorName.text = item.authorName


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    var onLongClick:((Review) -> Unit)? = null
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnLongClickListener{
            onLongClick?.invoke(items[position])
            true
        }
    }

    fun setData(newItems: List<Review>) {
        items = newItems
        notifyDataSetChanged()
    }
}