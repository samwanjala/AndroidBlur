package com.example.blurapp

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.example.blurapp.databinding.FragmentFirstBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.blur.setOnClickListener {
            binding.imageView.setImageBitmap(
               boxBlur(binding.imageView.drawable.toBitmap(), 20)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun boxBlur(input: Bitmap, radius: Int): Bitmap {
    Log.e("blurtask", "start")
    val blurBit = input.copy(input.config, true);
    var inPixels = IntArray(blurBit.width * blurBit.height)
    val outPixels = IntArray(blurBit.width * blurBit.height)
    blurBit.getPixels(inPixels, 0, blurBit.width, 0, 0, blurBit.width, blurBit.height)
    val div = radius * 2 + 1

    val blurThread = Thread {
        var hRTotal = 0
        var hGTotal = 0
        var hBTotal = 0
        var hATotal = 0
        var x = 0
        var leftPixel = 0
        var rightPixel = input.width - 1
        //horizontal pass
        for (p in inPixels.indices) {
            if(x == 0){
                hRTotal = 0
                hGTotal = 0
                hBTotal = 0
                hATotal = 0
                for (r in -radius..radius) {
                    val safeP = max(p + r, p)

                    val currentPixel = inPixels[safeP]

                    hRTotal += Color.red(currentPixel)
                    hGTotal += Color.green(currentPixel)
                    hBTotal += Color.blue(currentPixel)
                    hATotal += Color.alpha(currentPixel)
                }
                x++
            }else if(x < blurBit.width){
                val oldPixel = p - radius - 1
                val safeOldPixel = max(oldPixel, leftPixel)
                val discardPix = inPixels[safeOldPixel]

                val newPixel = p + radius
                val safeNewPixel = min(newPixel, rightPixel)
                val addPix = inPixels[safeNewPixel]

                hRTotal -= Color.red(discardPix)
                hRTotal += Color.red(addPix)

                hGTotal -= Color.green(discardPix)
                hGTotal += Color.green(addPix)

                hBTotal -= Color.blue(discardPix)
                hBTotal += Color.blue(addPix)

                hATotal -= Color.alpha(discardPix)
                hATotal += Color.alpha(addPix)
                x++
                if(x == blurBit.width){
                    x = 0
                    leftPixel += blurBit.width
                    rightPixel += blurBit.width
                }
            }

            val alpha = hATotal.floorDiv(div)
            val red = hRTotal.floorDiv(div)
            val green = hGTotal.floorDiv(div)
            val blue = hBTotal.floorDiv(div)

            val color = Color.argb(alpha, red, green, blue)

            outPixels[p] = color
        }

        inPixels = outPixels

        val totals = IntArray(blurBit.width * 4)
        var topPixel = 0
        var bottomPixel = inPixels.size - blurBit.width
        var offset = 0
        //vertical pass
        for (p in inPixels.indices) {
            if(p < blurBit.width){
                for (r in -radius..radius) {
                    val safeP = max(p + (blurBit.width * r), p)

                    val currentPixel = inPixels[safeP]

                    totals[p + offset] += Color.red(currentPixel)
                    totals[p + 1 + offset] += Color.green(currentPixel)
                    totals[p + 2 + offset] += Color.blue(currentPixel)
                    totals[p + 3 + offset] += Color.alpha(currentPixel)
                }
            }else{
                val oldPixel = p - (blurBit.width * (radius+1))
                val safeOldPixel = max(oldPixel, topPixel)
                val discardPix = inPixels[safeOldPixel]

                val newPixel = p + (blurBit.width * radius)
                val safeNewPixel = min(newPixel, bottomPixel)
                val addPix = inPixels[safeNewPixel]

                totals[topPixel + offset] -= Color.red(discardPix)
                totals[topPixel + offset] += Color.red(addPix)

                totals[topPixel + 1 + offset] -= Color.green(discardPix)
                totals[topPixel + 1 + offset] += Color.green(addPix)

                totals[topPixel + 2 + offset] -= Color.blue(discardPix)
                totals[topPixel + 2 + offset] += Color.blue(addPix)

                totals[topPixel + 3 + offset] -= Color.alpha(discardPix)
                totals[topPixel + 3 + offset] += Color.alpha(addPix)
            }

            val alpha = abs(totals[topPixel + 3 + offset]).floorDiv(div)

            val red = ((totals[topPixel + offset])/div)
                .coerceAtLeast(0)
                .coerceAtMost(255)
            val green = ((totals[topPixel + 1 + offset])/div)
                .coerceAtLeast(0)
                .coerceAtMost(255)
            val blue = ((totals[topPixel + 2 + offset])/div)
                .coerceAtLeast(0)
                .coerceAtMost(255)

            val color = Color.argb(alpha, red, green, blue)

            outPixels[p] = color

            topPixel++
            offset += 3
            if(topPixel == blurBit.width){
                topPixel = 0
                offset = 0
            }

            bottomPixel++
            if(bottomPixel == inPixels.size){
                bottomPixel = inPixels.size - blurBit.width
            }
        }

        blurBit.setPixels(outPixels, 0, blurBit.width, 0, 0, blurBit.width, blurBit.height)
    }

    blurThread.start()
    blurThread.join()
    Log.e("blurtask", "end")
    return blurBit.copy(input.config, false)
}

fun fastblur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
    Log.e("blurtask", "start")
    var sentBitmap = sentBitmap
    val width = Math.round(sentBitmap.width * scale)
    val height = Math.round(sentBitmap.height * scale)
    sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
    val bitmap = sentBitmap.copy(sentBitmap.config, true)
    if (radius < 1) {
        return null
    }
    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    Log.e("pix", w.toString() + " " + h + " " + pix.size)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)
    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1
    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    var yw: Int
    val vmin = IntArray(Math.max(w, h))
    var divsum = div + 1 shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    i = 0
    while (i < 256 * divsum) {
        dv[i] = i / divsum
        i++
    }
    yi = 0
    yw = yi
    val stack = Array(div) {
        IntArray(
            3
        )
    }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int
    y = 0
    while (y < h) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        i = -radius
        while (i <= radius) {
            p = pix[yi + Math.min(wm, Math.max(i, 0))]
            sir = stack[i + radius]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - Math.abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            i++
        }
        stackpointer = radius
        x = 0
        while (x < w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (y == 0) {
                vmin[x] = Math.min(x + radius + 1, wm)
            }
            p = pix[yw + vmin[x]]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi++
            x++
        }
        yw += w
        y++
    }
    x = 0
    while (x < w) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        yp = -radius * w
        i = -radius
        while (i <= radius) {
            yi = Math.max(0, yp) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - Math.abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += w
            }
            i++
        }
        yi = x
        stackpointer = radius
        y = 0
        while (y < h) {

            // Preserve alpha channel: ( 0xff000000 & pix[yi] )
            pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) {
                vmin[y] = Math.min(y + r1, hm) * w
            }
            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += w
            y++
        }
        x++
    }
    Log.e("pix", w.toString() + " " + h + " " + pix.size)
    bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    Log.e("blurtask", "end")
    return bitmap
}