package code.roy.retromusic.glide.artistimage

import android.content.Context
import code.roy.retromusic.network.DeezerService
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ArtistImageLoader(
    val context: Context,
    private val deezerService: DeezerService,
    private val okhttp: OkHttpClient,
) : ModelLoader<ArtistImage, InputStream> {

    override fun buildLoadData(
        model: ArtistImage,
        width: Int,
        height: Int,
        options: Options,
    ): LoadData<InputStream> {
        return LoadData(
            /* sourceKey = */ ObjectKey(model.artist.name),
            /* fetcher = */ ArtistImageFetcher(
                context = context,
                deezerService = deezerService,
                model = model,
                okhttp = okhttp
            )
        )
    }

    override fun handles(model: ArtistImage): Boolean {
        return true
    }
}

class Factory(
    val context: Context,
) : ModelLoaderFactory<ArtistImage, InputStream> {

    private var deezerService = DeezerService.invoke(
        DeezerService.createDefaultOkHttpClient(context)
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .addInterceptor(createLogInterceptor())
            .build()
    )

    private var okHttp = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
        .build()

    private fun createLogInterceptor(): Interceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ArtistImage, InputStream> {
        return ArtistImageLoader(
            context = context,
            deezerService = deezerService,
            okhttp = okHttp
        )
    }

    override fun teardown() {}

    companion object {
        // we need these very low values to make sure our artist image loading calls doesn't block the image loading queue
        private const val TIMEOUT: Long = 500
    }
}
