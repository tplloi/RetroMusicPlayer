package code.roy.retromusic.fragments

import androidx.annotation.LayoutRes
import code.roy.retromusic.R

enum class GridStyle constructor(
    @param:LayoutRes @field:LayoutRes val layoutResId: Int,
    val id: Int
) {
    Grid(R.layout.v_item_grid, 0),
    Card(R.layout.v_item_card, 1),
    ColoredCard(R.layout.v_item_card_color, 2),
    Circular(R.layout.v_item_grid_circle, 3),
    Image(R.layout.v_image, 4),
    GradientImage(R.layout.v_item_image_gradient, 5)
}