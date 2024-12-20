package com.dengzii.plugin.rbk.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun Entrance() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(10.dp).fillMaxWidth()
    ) {
        Text("生成的绑定方法名")
        TextField(value = "bindView", onValueChange = {}, label = { Text("生成的绑定方法名") })
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.background(Color.Cyan)
        ) {
            Checkbox(checked = true, onCheckedChange = { })
            Text("优先替换ButterKnife的bindView方法")
        }
        Text("在下列方法体内查找 ButterKnife.bind 方法，并替换为`bindView` 方法调用（使用\",\"分割）")
        TextField(value = "onCreate,onCreateView", onValueChange = {})
    }
}