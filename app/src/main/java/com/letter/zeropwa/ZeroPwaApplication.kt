package com.letter.zeropwa

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.letter.zeropwa.data.db.PwaRepository

class ZeroPwaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 配置全局 ImageLoader 支持 SVG 渲染
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)

        // 初始化多语言与主题配置状态
        com.letter.zeropwa.data.AppLanguage.initialize(this)
        com.letter.zeropwa.data.AppThemeState.init(this)

        // 预热 Repository 并同步动态快捷方式
        PwaRepository.getInstance(this)
    }
}
