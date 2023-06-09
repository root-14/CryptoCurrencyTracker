package com.root14.cryptocurrencytracker.ui.composable

import android.content.SharedPreferences
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.root14.cryptocurrencytracker.data.entity.Coin
import com.root14.cryptocurrencytracker.network.Status
import com.root14.cryptocurrencytracker.ui.theme.DarkBlack
import com.root14.cryptocurrencytracker.viewmodel.MainViewModel
import kotlinx.coroutines.delay

/**
 * Created by ilkay on 12,May, 2023
 */

@Composable
fun CoinDetailComposable(
    mainViewModel: MainViewModel = hiltViewModel(),
    coinId: String = "btc-bitcoin",
    sharedPreferences: SharedPreferences
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val coin by remember {
        mutableStateOf(Coin())
    }
    var isLoading0 by remember { mutableStateOf(true) }
    var isLoading1 by remember { mutableStateOf(true) }
    val imageState = remember { mutableStateOf<Drawable?>(null) }
    val backGroundWorkerPrice = remember { mutableStateOf<Double>(0.0) }

    LaunchedEffect(Unit) {
        mainViewModel.backGroundWorker.observe(lifecycleOwner) {
            backGroundWorkerPrice.value = it
            if (!coin.price.equals(it.toString())) {
                coin.price = it.toString()
            }
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.getCoinById(coinId).observe(lifecycleOwner) {

            sharedPreferences.edit().putString("backGroundWorker", coinId)
                .apply()//for background work

            when (it.status) {
                Status.SUCCESS -> {
                    coin.id = it.data?.id
                    coin.name = it.data?.name
                    coin.symbol = it.data?.symbol
                    coin.description = it.data?.description
                    coin.hashAlgorithm = it.data?.hashAlgorithm
                    coin.logoURL = it.data?.logoURL
                    isLoading0 = false

                    coin.logoURL?.let { it1 -> mainViewModel.loadImage(it1, imageState) }
                }

                Status.LOADING -> {/*loading yap*/
                    isLoading0 = true
                }


                Status.ERROR -> {
                    "cannot load detail ${it.message}"
                    isLoading0 = false
                }
            }
        }
        mainViewModel.getTickerById(coinId).observe(lifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    coin.price = it.data?.quotes?.USD?.price.toString()
                    coin.percentChange24h = it.data?.quotes?.USD?.change24H.toString()
                    isLoading1 = false
                }

                Status.LOADING -> {
                    isLoading1 = true
                }

                Status.ERROR -> {
                    "cannot load detail ${it.message}"
                    isLoading1 = false
                }
            }
        }
    }
    var updateIntervalSeconds by remember { mutableStateOf(60) }

    // This will automatically trigger a recomposition every `updateIntervalSeconds`
    LaunchedEffect(updateIntervalSeconds) {
        while (true) {
            delay(updateIntervalSeconds * 1000L)
            mainViewModel.getTickerById(coinId).observe(lifecycleOwner) {
                when (it.status) {
                    Status.SUCCESS -> {
                        coin.price = it.data?.quotes?.USD?.price.toString()
                        coin.percentChange24h = it.data?.quotes?.USD?.change24H.toString()
                        isLoading1 = false
                    }

                    Status.LOADING -> {
                        isLoading1 = true
                    }

                    Status.ERROR -> {
                        "cannot load detail ${it.message}"
                        isLoading1 = false
                    }
                }
            }
        }
    }

    Surface(color = DarkBlack) {}
    //loading screen
    if (isLoading0 or isLoading1) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.padding(top = 24.dp, bottom = 24.dp))
            androidx.compose.material3.Text(
                text = "Loading...",
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    } else {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "${coin.name} (${coin.hashAlgorithm})",
                style = MaterialTheme.typography.h4,
                color = Color.White
            )
            Text(
                text = coin.description!!,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.White
            )
            //image
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(36.dp)
            ) {
                if (imageState.value != null) {
                    Image(
                        bitmap = (imageState.value as BitmapDrawable).bitmap.asImageBitmap(),
                        contentDescription = "Image"
                    )
                } else {
                    CircularProgressIndicator()
                }

            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Price: ${coin.price}",
                    style = MaterialTheme.typography.h6,
                    color = Color.White, maxLines = 1
                )
                Text(
                    text = " (${coin.percentChange24h}%)",
                    style = MaterialTheme.typography.body1,
                    color = if ((coin.percentChange24h?.toIntOrNull()
                            ?: 0) >= 0
                    ) Color.Green else Color.Red,
                    modifier = Modifier.padding(start = 8.dp), maxLines = 1
                )
            }

            // Update interval
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Update interval (seconds):",
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.weight(1f),
                    color = Color.White
                )
                OutlinedTextField(
                    value = updateIntervalSeconds.toString(),
                    onValueChange = {
                        updateIntervalSeconds = it.toIntOrNull() ?: updateIntervalSeconds
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp),
                    singleLine = true
                )
                Button(
                    onClick = { /*onUpdateIntervalChange(updateIntervalSeconds) */ },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = "Apply")
                }
            }

            // Add to favorites button
            Button(onClick = { /*onAddToFavorites*/ }, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = "Add to favorites")
            }
        }
    }
}