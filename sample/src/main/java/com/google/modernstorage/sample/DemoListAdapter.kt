/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.modernstorage.sample

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.modernstorage.sample.databinding.DemoListItemBinding

/**
 * Contains details about a demo and required minSdk to work properly
 *
 * A snackBar is shown instead of navigating to the demo if the device has a lower sdk
 */
data class Demo(
    @DrawableRes val iconRes: Int,
    @StringRes val nameRes: Int,
    @IdRes val actionRes: Int,
    val minSdk: Int
)

/**
 * RecyclerView list adapter for demo
 */
class DemoListAdapter(private val dataSet: Array<Demo>) :
    RecyclerView.Adapter<DemoListAdapter.ViewHolder>() {

    class ViewHolder(viewBinding: DemoListItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        val binding = viewBinding
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            DemoListItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val textView = viewHolder.binding.textView
        val iconView = viewHolder.binding.iconView

        val context = textView.context
        iconView.setImageResource(dataSet[position].iconRes)
        textView.text = context.getString(dataSet[position].nameRes)
        viewHolder.binding.root.setOnClickListener { rowView ->
            if (Build.VERSION.SDK_INT >= dataSet[position].minSdk) {
                rowView.findNavController().navigate(dataSet[position].actionRes)
            } else {
                Snackbar.make(
                    rowView,
                    rowView.context.getString(
                        R.string.min_sdk_version_requirement,
                        dataSet[position].minSdk
                    ),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount() = dataSet.size
}
