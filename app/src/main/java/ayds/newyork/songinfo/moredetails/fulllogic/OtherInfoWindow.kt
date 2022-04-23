package ayds.newyork.songinfo.moredetails.fulllogic

import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle
import ayds.newyork.songinfo.R
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import com.google.gson.Gson
import com.google.gson.JsonObject
import android.content.Intent
import android.net.Uri
import com.squareup.picasso.Picasso
import android.view.View
import android.widget.ImageView
import androidx.core.text.HtmlCompat
import com.google.gson.JsonElement
import java.lang.StringBuilder

class OtherInfoWindow : AppCompatActivity() {
    private val logoNYT =
        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRVioI832nuYIXqzySD8cOXRZEcdlAj3KfxA62UEC4FhrHVe0f7oZXp3_mSFG7nIcUKhg&usqp=CAU"
    private val EMPTY_RESPONSE = "No Results"
    private val NYT_API_URL = "https://api.nytimes.com/svc/search/v2/"
    private val SECTION_DOCS = "docs"
    private val SECTION_ABSTRACT = "abstract"
    private val SECTION_WEB_URL = "web_url"
    private val SECTION_RESPONSE = "response"
    private lateinit var nytInfoPane: TextView
    private lateinit var dataBase: DataBase
    private lateinit var nytimesAPI: NYTimesAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_info)

        nytInfoPane = findViewById(R.id.NYTInfoPane)
        dataBase = openDataBase()

        prepareArtistInfoView(intent.getStringExtra(ARTIST_NAME_EXTRA)!!)
    }

    private fun openDataBase() = DataBase(this)

    private fun prepareArtistInfoView(artistName: String) {
        Thread {
            val infoDataBase = dataBase.getInfo(artistName)
            val artistInfo: String =
                getArtistInfoFromDataBase(infoDataBase) ?: getArtistInfoFromService(artistName)
            buildOtherInfoWindow(artistInfo)
        }.start()
    }

    private fun getArtistInfoFromDataBase(infoDB: String?): String? =
        if (infoDB != null) "[*]$infoDB" else null

    private fun getArtistInfoFromService(artistName: String): String {
        nytimesAPI = createRetrofit()
        val response = createArtistInfoJsonObject(artistName)
        val abstractNYT = response[SECTION_DOCS].asJsonArray[0].asJsonObject[SECTION_ABSTRACT]
        val artistInfoResult = getArtistInfo(abstractNYT, artistName)
        abstractNYT.let { dataBase.saveArtist(artistName, artistInfoResult) }
        val urlNYT = response[SECTION_DOCS].asJsonArray[0].asJsonObject[SECTION_WEB_URL]
        createURLButtonListener(urlNYT)
        return artistInfoResult
    }

    private fun createRetrofit(): NYTimesAPI {
        val retrofit = Retrofit.Builder()
            .baseUrl(NYT_API_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
        return retrofit.create(NYTimesAPI::class.java)
    }

    private fun createArtistInfoJsonObject(artistName: String): JsonObject {
        val callResponse = nytimesAPI.getArtistInfo(artistName).execute()
        val gson = Gson()
        val jobj = gson.fromJson(callResponse.body(), JsonObject::class.java)
        return jobj[SECTION_RESPONSE].asJsonObject
    }

    private fun getArtistInfo(abstractNYT: JsonElement?, artistName: String) =
        emptyResponse(abstractNYT) ?: getfromService(abstractNYT, artistName)

    private fun emptyResponse(abstractNYT: JsonElement?) = if (abstractNYT == null) EMPTY_RESPONSE else null

    private fun getfromService(abstractNYT: JsonElement?, artistName: String): String {
        val artistInfoFromService = abstractNYT!!.asString.replace("\\n", "\n")
        val artistInfoResult = artistNameToHtml(artistInfoFromService, artistName)
        return artistInfoResult
    }

    private fun artistNameToHtml(NYTinfo: String, artistName: String): String {
        val builder = StringBuilder()
        builder.append("<html><div width=400>")
        builder.append("<font face=\"arial\">")
        val textWithBold = NYTinfo
            .replace("'", " ")
            .replace("\n", "<br>")
            .replace("(?i)" + artistName.toRegex(), "<b>" + artistName.uppercase() + "</b>")
        builder.append(textWithBold)
        builder.append("</font></div></html>")
        return builder.toString()
    }

    private fun createURLButtonListener(urlNYT: JsonElement) {
        val urlString = urlNYT.asString
        findViewById<View>(R.id.openUrlButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(urlString)
            this.startActivity(intent)
        }
    }

    private fun buildOtherInfoWindow(artistInfo: String) {
        runOnUiThread {
            Picasso.get().load(logoNYT).into(findViewById<View>(R.id.imageView) as ImageView)
            nytInfoPane.text =
                HtmlCompat.fromHtml(artistInfo, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    companion object {
        const val ARTIST_NAME_EXTRA = "artistName"
    }
}