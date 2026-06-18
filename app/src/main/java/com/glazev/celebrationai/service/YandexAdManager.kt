package com.glazev.celebrationai.service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

import com.glazev.celebrationai.data.AppSettings

class YandexAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var rewardedAdLoader: RewardedAdLoader? = null
    private val appSettings = AppSettings(context)

    init {
        rewardedAdLoader = RewardedAdLoader(context).apply {
            setAdLoadListener(object : RewardedAdLoadListener {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d("YandexAdManager", "Rewarded Ad Loaded")
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e("YandexAdManager", "Rewarded Ad Failed to Load: ${error.description}")
                }
            })
        }
        loadRewardedAd()
    }

    private fun loadRewardedAd() {
        rewardedAdLoader?.loadAd(
            AdRequestConfiguration.Builder("R-M-19452535-1").build()
        )
    }

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() { }
                override fun onAdFailedToShow(error: AdError) {
                    onFailed()
                    loadRewardedAd()
                }
                override fun onAdDismissed() {
                    loadRewardedAd()
                }
                override fun onAdClicked() { }
                override fun onAdImpression(data: ImpressionData?) { }
                override fun onRewarded(reward: Reward) {
                    onRewarded()
                }
            })
            rewardedAd?.show(activity)
            rewardedAd = null
        } else {
            // Если реклама не загружена, разрешаем действие (или можно выдать ошибку)
            onRewarded()
            loadRewardedAd()
        }
    }
}
