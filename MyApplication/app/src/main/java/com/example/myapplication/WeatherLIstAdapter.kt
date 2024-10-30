package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WeatherListItem(
    val contents: String,
    val weather: Int,
    val time: String,
)

class WeatherListAdapter(
    private val items: MutableList<WeatherListItem>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<WeatherListAdapter.WeatherListViewHolder>() {

    inner class WeatherListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contents: TextView = itemView.findViewById(R.id.weatherlist_item_contents)
        private val weather: ImageView = itemView.findViewById(R.id.weatherlist_item_icon)
        private val time: TextView = itemView.findViewById(R.id.weatherlist_time)
        private val deleteButton: Button = itemView.findViewById(R.id.weatherlist_item_delete)

        fun bind(item: WeatherListItem, position: Int) {
            contents.text = item.contents
            weather.setImageResource(item.weather) // 저장된 drawable ID로 이미지 설정
            time.text = item.time

            deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weatherlist, parent, false)
        return WeatherListViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherListViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size
}
