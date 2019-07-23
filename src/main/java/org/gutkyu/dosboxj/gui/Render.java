package org.gutkyu.dosboxj.gui;

import org.gutkyu.dosboxj.util.*;
import java.awt.Color;
import java.util.Arrays;
import org.gutkyu.dosboxj.DOSBox;
import org.gutkyu.dosboxj.gui.GFXCallback.GFXCallbackType;
import org.gutkyu.dosboxj.misc.*;
import org.gutkyu.dosboxj.misc.setup.*;
import org.gutkyu.dosboxj.misc.setup.Value.WrongType;

/// RENDER_USE_ADVANCED_SCALERS = 0
///
/// 스케일 출력모드를 32bpp로 고정
/// - scale_outMode = scalerMode.scalerMode32
/// - 당연히 RenderPal_t.lut도 int배열만으로 구성
public final class Render {
    private static Render _render = new Render(GUIPlatform.gfx);

    public static Render instance() {
        return _render;
    }

    public final short RENDER_SKIP_CACHE = 16;

    public ScalerLineHandler DrawLine;

    private IGFX _video = null;

    private static final int SIZE_UINT = 4;
    private static final int SIZE_USHORT = 2;
    private static final int SIZE_UBYTE = 1;

    private Render(IGFX video) {
        _video = video;

        ScaleNormal1x = new ScalerSimpleBlock();

        ScaleNormal1x.name = "Normal";
        ScaleNormal1x.gfxFlags = GFXFlag.CAN8 | GFXFlag.CAN15 | GFXFlag.CAN16 | GFXFlag.CAN32;
        ScaleNormal1x.xscale = 1;
        ScaleNormal1x.yscale = 1;
        ScaleNormal1x.Linear = new ScalerLineHandler[][] {{null, null, null, this::Normal1x_8_32_L},
                {null, null, null, this::Normal1x_15_32_L},
                {null, null, null, this::Normal1x_16_32_L},
                {null, null, null, this::Normal1x_32_32_L},
                {null, null, null, this::Normal1x_9_32_L}};
        ScaleNormal1x.Random = new ScalerLineHandler[][] {{null, null, null, this::Normal1x_8_32_R},
                {null, null, null, this::Normal1x_15_32_R},
                {null, null, null, this::Normal1x_16_32_R},
                {null, null, null, this::Normal1x_32_32_R},
                {null, null, null, this::Normal1x_9_32_R}};

        ScaleNormal2x = new ScalerSimpleBlock();

        ScaleNormal2x.name = "Normal2x";
        ScaleNormal2x.gfxFlags = GFXFlag.CAN8 | GFXFlag.CAN15 | GFXFlag.CAN16 | GFXFlag.CAN32;
        ScaleNormal2x.xscale = 2;
        ScaleNormal2x.yscale = 2;
        ScaleNormal2x.Linear = new ScalerLineHandler[][] {{null, null, null, this::Normal2x_8_32_L},
                {null, null, null, this::Normal2x_15_32_L},
                {null, null, null, this::Normal2x_16_32_L},
                {null, null, null, this::Normal2x_32_32_L},
                {null, null, null, this::Normal2x_9_32_L}};
        ScaleNormal2x.Random = new ScalerLineHandler[][] {{null, null, null, this::Normal2x_8_32_R},
                {null, null, null, this::Normal2x_15_32_R},
                {null, null, null, this::Normal2x_16_32_R},
                {null, null, null, this::Normal2x_32_32_R},
                {null, null, null, this::Normal2x_9_32_R},};

    }

    private void increaseFrameSkip(boolean pressed) {
        if (!pressed)
            return;
        if (_frameSkipMax < 10)
            _frameSkipMax++;
        Log.logMsg("Frame Skip at %d", _frameSkipMax);
        // _video.GFX_SetTitle(-1, frameskip_max, false);
    }

    private void decreaseFrameSkip(boolean pressed) {
        if (!pressed)
            return;
        if (_frameSkipMax > 0)
            _frameSkipMax--;
        Log.logMsg("Frame Skip at %d", _frameSkipMax);
        // _video.GFX_SetTitle(-1, frameskip_max, false);
    }
    /*
     * Disabled as I don't want to waste a keybind for that. Might be used in the future (Qbix)
     * private static void ChangeScaler(boolean pressed) { if (!pressed) return; scale.op =
     * (scalerOperation)((int)scale.op+1); if((scale.op) >= scalerLast || scale_size == 1) {
     * scale.op = (scalerOperation)0; if(++scale_size > 3) scale_size = 1; } RENDER_CallBack(
     * GFX_CallBackReset ); }
     */

    private void checkPalette() {
        /* Clean up any previous changed palette data */
        if (_pal.changed) {
            Arrays.fill(_pal.modified, 0, _pal.modified.length, (byte) 0);
            _pal.changed = false;
        }
        if (_pal.first > _pal.last)
            return;
        int i;
        switch (_scaleOutMode) {

            case SCALER_MODE8:
            default:
                for (i = _pal.first; i <= _pal.last; i++) {
                    Color rgb = _pal.rgb[i];
                    int newPal = _video.getRGB(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
                    if (newPal != _pal.lut[i]) {
                        _pal.changed = true;
                        _pal.modified[i] = 1;
                        _pal.lut[i] = newPal;
                        _pal.lutBytes[i] = new byte[] {(byte) rgb.getRed(), (byte) rgb.getGreen(),
                                (byte) rgb.getBlue(), (byte) 0xff};
                    }
                }
                break;
        }
        /* Setup pal index to startup values */
        _pal.first = 256;
        _pal.last = 0;
    }

    private int makeAspectTable(int skip, int height, double scaley, int miny) {
        int i;
        double lines = 0;
        int linesadded = 0;
        for (i = 0; i < skip; i++)
            Scaler_Aspect[i] = 0;

        height += skip;
        for (i = skip; i < height; i++) {
            lines += scaley;
            if (lines >= miny) {
                int templines = (int) lines;
                lines -= templines;
                linesadded += templines;
                Scaler_Aspect[i] = (byte) templines;
            } else {
                Scaler_Aspect[i] = 0;
            }
        }
        return linesadded;
    }

    // -- #region RENDER_prefix
    public void setPal(int entry, int red, int green, int blue) {
        _pal.rgb[entry] = new Color(red, green, blue);
        if (_pal.first > entry)
            _pal.first = entry;
        if (_pal.last < entry)
            _pal.last = entry;
    }

    private ScalerLineHandler emptyLineHandlerWrap = this::emptyLineHandler;

    private void emptyLineHandler(byte[] src, int index) {
    }


    // private void StartLineHandler(byte[] src, uint index)
    ScalerLineHandler startLineHandlerWrap = this::startLineHandler;

    private void startLineHandler(byte[] src, int index) {
        if (src != null) {
            int sidx = index, cidx = _scaleCacheReadIndex;
            byte[] cache = _scaleCacheRead;
            for (int x = _srcStart; x > 0;) {
                if (src[sidx] != cache[cidx] || src[sidx + 1] != cache[cidx + 1]
                        || src[sidx + 2] != cache[cidx + 2] || src[sidx + 3] != cache[cidx + 3]) {
                    if (!_video.startUpdate()) {
                        DrawLine = emptyLineHandlerWrap;
                        return;
                    }
                    _scaleOutWrite = _video.getPixels();
                    _scaleOutWriteIndex = (int) _video.getCurrentPixelIndex();
                    _scaleOutPitch = _video.getPitch();
                    _scaleOutWriteIndex += _scaleOutPitch * Scaler_ChangedLines[0];
                    DrawLine = _scaleLineHandler;
                    DrawLine.draw(src, index);
                    return;
                }
                x--;
                sidx += 4;
                cidx += 4;
            }
        }
        _scaleCacheReadIndex += _scaleCachePitch;
        Scaler_ChangedLines[0] += Scaler_Aspect[_scaleInLine];
        _scaleInLine++;
        _scaleOutLine++;
    }

    // private void FinishLineHandler(byte[] src, int index)
    ScalerLineHandler finishLineHandlerWrap = this::finishLineHandler;

    private void finishLineHandler(byte[] src, int index) {
        if (src != null) {
            ArrayHelper.copy(src, index, _scaleCacheRead, _scaleCacheReadIndex, _srcStart * 4);// sizeof(int)
        }
        _scaleCacheReadIndex += _scaleCachePitch;
    }

    ScalerLineHandler clearCacheHandlerWrap = this::clearCacheHandler;

    private void clearCacheHandler(byte[] src, int index) {
        int idx;
        int srcLineIdx = index, cacheLineIdx = _scaleCacheReadIndex;
        // 소스의 bpp가 32일 경우를 전제로 계산하는듯, 그러나 구형 PC의 가로 픽셀 수는 4의
        // 배수(허큘리스:720,CGA:320,EGA:640,VGA:640)이므로 4로 나누어 처리해도 문제가 되지 않음
        // width = scale_cachePitch / 4; //src_start와 동일
        for (idx = 0; idx < _scaleCachePitch; idx++) {
            _scaleCacheRead[cacheLineIdx + idx] = (byte) ~src[srcLineIdx + idx];
        }
        _scaleLineHandler.draw(src, index);
    }

    public boolean startUpdate() {
        if (_updating)
            return false;
        if (!_active)
            return false;
        if (_frameSkipCount < _frameSkipMax) {
            _frameSkipCount++;
            return false;
        }
        _frameSkipCount = 0;
        if (_scaleInMode == SCALER_MODE8) {
            checkPalette();
        }
        _scaleInLine = 0;
        _scaleOutLine = 0;
        _scaleCacheRead = scalerSourceCache;
        _scaleCacheReadIndex = scalerSourceCacheIndex;
        _scaleOutWrite = null;
        _scaleOutWriteIndex = 0;
        _scaleOutPitch = 0;
        Scaler_ChangedLines[0] = 0;
        Scaler_ChangedLineIndex = 0;
        /*
         * Clearing the cache will first process the line to make sure it's never the same
         */
        if (_scaleClearCache) {
            // Log.LOG_MSG("Clearing cache");
            // Will always have to update the screen with this one anyway, so let's update
            // already
            if (!_video.startUpdate())
                return false;
            _scaleOutWrite = _video.getPixels();
            _scaleOutWriteIndex = (int) _video.getCurrentPixelIndex();
            _scaleOutPitch = _video.getPitch();

            _fullFrame = true;
            _scaleClearCache = false;
            DrawLine = clearCacheHandlerWrap;
        } else {
            if (_pal.changed) {
                /* Assume pal changes always do a full screen update anyway */
                if (!_video.startUpdate())
                    return false;
                _scaleOutWrite = _video.getPixels();
                _scaleOutWriteIndex = (int) _video.getCurrentPixelIndex();
                _scaleOutPitch = _video.getPitch();
                DrawLine = _scaleLinePalHandler;
                _fullFrame = true;
            } else {
                DrawLine = startLineHandlerWrap;
                // if ((CaptureState & (CAPTURE_IMAGE|CAPTURE_VIDEO)))
                // fullFrame = true;
                // else
                _fullFrame = false;
            }
        }
        _updating = true;
        return true;
    }

    private void halt() {
        DrawLine = emptyLineHandlerWrap;
        _video.endUpdate(null);
        _updating = false;
        _active = false;
    }

    public void endUpdate(boolean abort) {
        if (!_updating)
            return;
        DrawLine = emptyLineHandlerWrap;
        // if (GCC_UNLIKELY(CaptureState & (CAPTURE_IMAGE|CAPTURE_VIDEO))) {
        // int pitch, flags;
        // flags = 0;
        // if (src_dblw != src_dblh) {
        // if (src_dblw) flags|=CAPTURE_FLAG_DBLW;
        // if (src_dblh) flags|=CAPTURE_FLAG_DBLH;
        // }
        // float fps = src.fps;
        // pitch = scale_cachePitch;
        // if (frameskip_max)
        // fps /= 1+frameskip_max;
        // CAPTURE_AddImage( src_width, src_height, src.bpp, pitch,
        // flags, fps, (byte *)&scalerSourceCache, (byte*)&pal.rgb );
        // }
        if (_scaleOutWrite != null) {
            _video.endUpdate(abort ? null : Scaler_ChangedLines);
            _frameSkipHadSkip[_frameSkipIndex] = 0;
        } else {
            // #if 0
            // int total = 0, i;
            // frameskip_hadSkip[frameskip_index] = 1;
            // for (i = 0;i<RENDER_SKIP_CACHE;i++)
            // total += frameskip_hadSkip[i];
            // Log.LOG_MSG( "Skipped frame %d %d", PIC_Ticks, (total * 100) /
            // RENDER_SKIP_CACHE );
            // #endif
        }
        _frameSkipIndex = (_frameSkipIndex + 1) & (RENDER_SKIP_CACHE - 1);
        _updating = false;
    }

    private void reset() {
        int width = _srcWidth;
        int height = _srcHeight;
        boolean dblw = _srcDblw;
        boolean dblh = _srcDblh;

        double gfx_scalew;
        double gfx_scaleh;

        int gfx_flags;
        int xscale, yscale;
        ScalerSimpleBlock simpleBlock = ScaleNormal1x;
        ScalerComplexBlock complexBlock = null;
        if (_aspect) {
            if (_srcRatio > 1.0) {
                gfx_scalew = 1;
                gfx_scaleh = _srcRatio;
            } else {
                gfx_scalew = (1 / _srcRatio);
                gfx_scaleh = 1;
            }
        } else {
            gfx_scalew = 1;
            gfx_scaleh = 1;
        }
        if ((dblh && dblw) || (_scaleForced && !dblh && !dblw)) {
            /* Initialize always working defaults */
            if (_scaleSize == 2)
                simpleBlock = ScaleNormal2x;
            else if (_scaleSize == 3)
                // simpleBlock = ScaleNormal3x;
                throw new DOSException("3x scale 미구현");
            else
                simpleBlock = ScaleNormal1x;
            /* Maybe override them */
            // #if RENDER_USE_ADVANCED_SCALERS>0
            // switch (scale.op) {
            // #if RENDER_USE_ADVANCED_SCALERS>2
            // case scalerOpAdvInterp:
            // if (scale_size == 2)
            // complexBlock = &ScaleAdvInterp2x;
            // else if (scale_size == 3)
            // complexBlock = &ScaleAdvInterp3x;
            // break;
            // case scalerOpAdvMame:
            // if (scale_size == 2)
            // complexBlock = &ScaleAdvMame2x;
            // else if (scale_size == 3)
            // complexBlock = &ScaleAdvMame3x;
            // break;
            // case scalerOpHQ:
            // if (scale_size == 2)
            // complexBlock = &ScaleHQ2x;
            // else if (scale_size == 3)
            // complexBlock = &ScaleHQ3x;
            // break;
            // case scalerOpSuperSaI:
            // if (scale_size == 2)
            // complexBlock = &ScaleSuper2xSaI;
            // break;
            // case scalerOpSuperEagle:
            // if (scale_size == 2)
            // complexBlock = &ScaleSuperEagle;
            // break;
            // case scalerOpSaI:
            // if (scale_size == 2)
            // complexBlock = &Scale2xSaI;
            // break;
            // #endif
            // case scalerOpTV:
            // if (scale_size == 2)
            // simpleBlock = &ScaleTV2x;
            // else if (scale_size == 3)
            // simpleBlock = &ScaleTV3x;
            // break;
            // case scalerOpRGB:
            // if (scale_size == 2)
            // simpleBlock = &ScaleRGB2x;
            // else if (scale_size == 3)
            // simpleBlock = &ScaleRGB3x;
            // break;
            // case scalerOpScan:
            // if (scale_size == 2)
            // simpleBlock = &ScaleScan2x;
            // else if (scale_size == 3)
            // simpleBlock = &ScaleScan3x;
            // break;
            // default:
            // break;
            // }
            // #endif
        } else if (dblw) {
            // simpleBlock = ScaleNormalDw;
            throw new DOSException("ScaleNormalDw 미구현");
        } else if (dblh) {
            // simpleBlock = ScaleNormalDh;
            throw new DOSException("ScaleNormalDh 미구현");
        } else {

            complexBlock = null;
            simpleBlock = ScaleNormal1x;
        }
        forcenormal: // goto문 제약때문에 원 소스와 다르게 수정
        while (true) {
            if (complexBlock != null) {
                // #if RENDER_USE_ADVANCED_SCALERS>1
                // if ((width >= SCALER_COMPLEXWIDTH - 16) || height >= SCALER_COMPLEXHEIGHT -
                // 16) {
                // Log.LOG_MSG("Scaler can't handle this resolution, going back to normal");
                // goto forcenormal;
                // }
                // #else
                {
                    // goto문 제약때문에 원 소스와 다르게 수정
                    complexBlock = null;
                    simpleBlock = ScaleNormal1x;
                    continue forcenormal;// goto forcenormal;
                }
                // #endif
                // gfx_flags = complexBlock.gfxFlags;
                // xscale = complexBlock.xscale;
                // yscale = complexBlock.yscale;
                // Log.LOG_MSG("Scaler:%s",complexBlock.name);
            } else {
                gfx_flags = simpleBlock.gfxFlags;
                xscale = simpleBlock.xscale;
                yscale = simpleBlock.yscale;
                // Log.LOG_MSG("Scaler:%s",simpleBlock.name);
            }
            switch (_srcBpp) {
                case 8:
                    _srcStart = (_srcWidth * 1) / SIZE_UINT;
                    if ((gfx_flags & GFXFlag.CAN8) == GFXFlag.CAN8)
                        gfx_flags |= GFXFlag.LOVE8;
                    else
                        gfx_flags |= GFXFlag.LOVE32;
                    break;
                case 15:
                    _srcStart = (_srcWidth * 2) / SIZE_UINT;
                    gfx_flags |= GFXFlag.LOVE15;
                    gfx_flags = (gfx_flags & ~GFXFlag.CAN8) | GFXFlag.RGBONLY;
                    break;
                case 16:
                    _srcStart = (_srcWidth * 2) / SIZE_UINT;
                    gfx_flags |= GFXFlag.LOVE16;
                    gfx_flags = (gfx_flags & ~GFXFlag.CAN8) | GFXFlag.RGBONLY;
                    break;
                case 32:
                    _srcStart = (_srcWidth * 4) / SIZE_UINT;
                    gfx_flags |= GFXFlag.LOVE32;
                    gfx_flags = (gfx_flags & ~GFXFlag.CAN8) | GFXFlag.RGBONLY;
                    break;
            }
            gfx_flags = _video.getBestMode(gfx_flags);
            if (gfx_flags == 0) {
                if (complexBlock == null && simpleBlock == ScaleNormal1x)
                    Support.exceptionExit("Failed to create a rendering output");
                else {
                    complexBlock = null;
                    simpleBlock = ScaleNormal1x;
                    continue forcenormal;// goto forcenormal;
                }
            }
            break;
        }
        width *= xscale;
        int skip = complexBlock != null ? 1 : 0;
        if ((gfx_flags & GFXFlag.SCALING) == GFXFlag.SCALING) {
            height = makeAspectTable(skip, _srcHeight, yscale, yscale);
        } else {
            if ((gfx_flags & GFXFlag.CAN_RANDOM) == GFXFlag.CAN_RANDOM && gfx_scaleh > 1) {
                gfx_scaleh *= yscale;
                height = makeAspectTable(skip, _srcHeight, gfx_scaleh, yscale);
            } else {
                gfx_flags &= ~GFXFlag.CAN_RANDOM; // Hardware surface when possible
                height = makeAspectTable(skip, _srcHeight, yscale, yscale);
            }
        }
        /* Setup the scaler variables */
        gfx_flags = _video.setSize(width, height, gfx_flags, gfx_scalew, gfx_scaleh, callbackWrap);
        if ((gfx_flags & GFXFlag.CAN8) == GFXFlag.CAN8)
            _scaleOutMode = SCALER_MODE8;
        else if ((gfx_flags & GFXFlag.CAN15) == GFXFlag.CAN15)
            _scaleOutMode = SCALER_MODE15;
        else if ((gfx_flags & GFXFlag.CAN16) == GFXFlag.CAN16)
            _scaleOutMode = SCALER_MODE16;
        else if ((gfx_flags & GFXFlag.CAN32) == GFXFlag.CAN32)
            _scaleOutMode = SCALER_MODE32;
        else
            Support.exceptionExit("Failed to create a rendering output");
        ScalerLineHandler[][] lineBlock;
        if ((gfx_flags & GFXFlag.HARDWARE) == GFXFlag.HARDWARE) {
            // #if RENDER_USE_ADVANCED_SCALERS>1
            // if (complexBlock) {
            // lineBlock = &ScalerCache;
            // scale_complexHandler = complexBlock.Linear[ scale_outMode ];
            // } else
            // #endif
            {
                _scaleComplexHandler = null;
                lineBlock = simpleBlock.Linear;
            }
        } else {
            // #if RENDER_USE_ADVANCED_SCALERS>1
            // if (complexBlock) {
            // lineBlock = &ScalerCache;
            // scale.complexHandler = complexBlock.Random[ scale_outMode ];
            // } else
            // #endif
            {
                _scaleComplexHandler = null;
                lineBlock = simpleBlock.Random;
            }
        }
        switch (_srcBpp) {
            case 8:
                _scaleLineHandler = lineBlock[0][_scaleOutMode];
                _scaleLinePalHandler = lineBlock[4][_scaleOutMode];
                _scaleInMode = SCALER_MODE8;
                _scaleCachePitch = _srcWidth * 1;
                break;
            case 15:
                _scaleLineHandler = lineBlock[1][_scaleOutMode];
                _scaleLinePalHandler = null;
                _scaleInMode = SCALER_MODE15;
                _scaleCachePitch = _srcWidth * 2;
                break;
            case 16:
                _scaleLineHandler = lineBlock[2][_scaleOutMode];
                _scaleLinePalHandler = null;
                _scaleInMode = SCALER_MODE16;
                _scaleCachePitch = _srcWidth * 2;
                break;
            case 32:
                _scaleLineHandler = lineBlock[3][_scaleOutMode];
                _scaleLinePalHandler = null;
                _scaleInMode = SCALER_MODE32;
                _scaleCachePitch = _srcWidth * 4;
                break;
            default:
                Support.exceptionExit("RENDER:Wrong source bpp %d", _srcBpp);
                break;
        }
        _scaleBlocks = _srcWidth / SCALER_BLOCKSIZE;
        _scaleLastBlock = _srcWidth % SCALER_BLOCKSIZE;
        _scaleInHeight = _srcHeight;
        /* Reset the palette change detection to it's initial value */
        _pal.first = 0;
        _pal.last = 255;
        _pal.changed = false;
        Arrays.fill(_pal.modified, 0, _pal.modified.length, (byte) 0);
        // Finish this frame using a copy only handler
        DrawLine = finishLineHandlerWrap;
        _scaleOutWrite = null;
        _scaleOutWriteIndex = 0;
        /* Signal the next frame to first reinit the cache */
        _scaleClearCache = true;
        _active = true;
    }

    GFXCallback callbackWrap = this::callBack;

    private void callBack(GFXCallbackType function) {
        if (function == GFXCallbackType.Stop) {
            halt();
            return;
        }
        // GFX_Events에서 VGA_Expose를 할일이 없기 때문에 RENDER_ClearCacheHandler를 사용할 일이 없음
        // 아마도 SDL 창이 숨겨졌다 나타날때 다시 그리는 부분이 아닐까?
        else if (function == GFXCallbackType.Redraw) {
            _scaleClearCache = true;
            return;
        } else if (function == GFXCallbackType.Reset) {
            _video.endUpdate(null);
            reset();
        } else {
            Support.exceptionExit("Unhandled GFX_CallBackReset %d", function.name());
        }
    }

    public void setSize(int width, int height, int bpp, float fps, double ratio, boolean dblw,
            boolean dblh) {
        halt();
        if (width == 0 || height == 0 || width > SCALER_MAXWIDTH || height > SCALER_MAXHEIGHT) {
            return;
        }
        if (ratio > 1) {
            double target = height * ratio + 0.1;
            ratio = target / height;
        } else {
            // This would alter the width of the screen, we don't care about rounding errors
            // here
        }
        _srcWidth = width;
        _srcHeight = height;
        _srcBpp = bpp;
        _srcDblw = dblw;
        _srcDblh = dblh;
        _srcFps = fps;
        _srcRatio = ratio;
        reset();
    }

    // For restarting the renderer.
    private static boolean running = false;

    public void init(Section sec) throws WrongType {
        SectionProperty section = (SectionProperty) (sec);

        // static boolean running = false;
        boolean aspect1 = _aspect;
        int scalersize = _scaleSize;
        boolean scalerforced = _scaleForced;
        scalerOperation scalerOp = _scaleOp;

        _pal.first = 256;
        _pal.last = 0;
        aspect1 = section.getBool("aspect");
        _frameSkipMax = section.getInt("frameskip");
        _frameSkipCount = 0;
        String cline = "";
        String scaler;
        // Check for commandline paramters and parse them through the configclass so they get
        // checked against allowed values
        if ((cline = DOSBox.Control.CmdLine.findString("-scaler", false)) != null) {
            section.handleInputline("scaler=" + cline);
        } else if ((cline = DOSBox.Control.CmdLine.findString("-forcescaler", false)) != null) {
            section.handleInputline("scaler=" + cline + " forced");
        }

        PropertyMultival prop = section.getMultival("scaler");
        scaler = prop.getSection().getString("type");
        String f = prop.getSection().getString("force");
        _scaleForced = false;
        if (f == "forced")
            _scaleForced = true;

        if (scaler == "none") {
            _scaleOp = scalerOperation.scalerOpNormal;
            _scaleSize = 1;
        } else if (scaler == "normal2x") {
            _scaleOp = scalerOperation.scalerOpNormal;
            _scaleSize = 2;
        } else if (scaler == "normal3x") {
            _scaleOp = scalerOperation.scalerOpNormal;
            _scaleSize = 3;
        }
        // #if RENDER_USE_ADVANCED_SCALERS>2
        // else if (scaler == "advmame2x") { scale.op = scalerOpAdvMame;scale_size = 2; }
        // else if (scaler == "advmame3x") { scale.op = scalerOpAdvMame;scale_size = 3; }
        // else if (scaler == "advinterp2x") { scale.op = scalerOpAdvInterp;scale_size = 2; }
        // else if (scaler == "advinterp3x") { scale.op = scalerOpAdvInterp;scale_size = 3; }
        // else if (scaler == "hq2x") { scale.op = scalerOpHQ;scale_size = 2; }
        // else if (scaler == "hq3x") { scale.op = scalerOpHQ;scale_size = 3; }
        // else if (scaler == "2xsai") { scale.op = scalerOpSaI;scale_size = 2; }
        // else if (scaler == "super2xsai") { scale.op = scalerOpSuperSaI;scale_size = 2; }
        // else if (scaler == "supereagle") { scale.op = scalerOpSuperEagle;scale_size = 2; }
        // #endif
        // #if RENDER_USE_ADVANCED_SCALERS>0
        // else if (scaler == "tv2x") { scale.op = scalerOpTV;scale_size = 2; }
        // else if (scaler == "tv3x") { scale.op = scalerOpTV;scale_size = 3; }
        // else if (scaler == "rgb2x"){ scale.op = scalerOpRGB;scale_size = 2; }
        // else if (scaler == "rgb3x"){ scale.op = scalerOpRGB;scale_size = 3; }
        // else if (scaler == "scan2x"){ scale.op = scalerOpScan;scale_size = 2; }
        // else if (scaler == "scan3x"){ scale.op = scalerOpScan;scale_size = 3; }
        // #endif

        // If something changed that needs a ReInit
        // Only ReInit when there is a src.bpp (fixes crashes on startup and directly changing the
        // scaler without a screen specified yet)
        if (running && _srcBpp != 0 && ((_aspect != aspect1) || (scalerOp != _scaleOp)
                || (_scaleSize != scalersize) || (_scaleForced != scalerforced) || _scaleForced))
            callBack(GFXCallbackType.Reset);

        if (!running)
            _updating = true;
        running = true;

        GUIPlatform.mapper.addKeyHandler(this::decreaseFrameSkip, MapKeys.F7, Mapper.MMOD1,
                "decfskip", "Dec Fskip");
        GUIPlatform.mapper.addKeyHandler(this::increaseFrameSkip, MapKeys.F8, Mapper.MMOD1,
                "incfskip", "Inc Fskip");
        // _video.GFX_SetTitle(-1,frameskip_max,false);
    }

    // -- #endregion
    /*--------------------------- begin Render_Scalers -----------------------------*/
    // reduced to save some memory
    public static final short SCALER_MAXWIDTH = 800;
    public static final short SCALER_MAXHEIGHT = 600;
    // #endif

    // #if RENDER_USE_ADVANCED_SCALERS>1
    // public final short SCALER_COMPLEXWIDTH =800;
    // public final short SCALER_COMPLEXHEIGHT =600;
    // #endif

    public final short SCALER_BLOCKSIZE = 16;

    private static final int SCALER_MODE8 = 0;
    private static final int SCALER_MODE15 = 1;
    private static final int SCALER_MODE16 = 2;
    private static final int SCALER_MODE32 = 3;


    enum scalerOperation {
        scalerOpNormal,
        // #if RENDER_USE_ADVANCED_SCALERS>2
        // scalerOpAdvMame,
        // scalerOpAdvInterp,
        // scalerOpHQ,
        // scalerOpSaI,
        // scalerOpSuperSaI,
        // scalerOpSuperEagle,
        // #endif
        // #if RENDER_USE_ADVANCED_SCALERS>0
        // scalerOpTV,
        // scalerOpRGB,
        // scalerOpScan,
        // #endif
        scalerLast
    };

    // #if RENDER_USE_ADVANCED_SCALERS>1
    /// * Not entirely happy about those +2's since they make a non power of 2, with muls instead of
    // shift */
    // typedef Bit8u scalerChangeCache_t [SCALER_COMPLEXHEIGHT][SCALER_COMPLEXWIDTH /
    // SCALER_BLOCKSIZE] ;
    // typedef union {
    // Bit32u b32 [SCALER_COMPLEXHEIGHT] [SCALER_COMPLEXWIDTH];
    // Bit16u b16 [SCALER_COMPLEXHEIGHT] [SCALER_COMPLEXWIDTH];
    // Bit8u b8 [SCALER_COMPLEXHEIGHT] [SCALER_COMPLEXWIDTH];
    // } scalerFrameCache_t;
    // #endif
    // 8, 16, 32bit 를 모두 포함하기 위해 가장 큰 자료구조를 기준으로 생성
    byte[] scalerSourceCache = new byte[SCALER_MAXHEIGHT * SCALER_MAXWIDTH * SIZE_UINT];
    int scalerSourceCacheIndex = 0;// uint
    // #if RENDER_USE_ADVANCED_SCALERS>1
    // extern scalerChangeCache_t scalerChangeCache;
    // #endif

    private static final class ScalerComplexBlock {
        public String name;
        public int gfxFlags;
        public int xscale, yscale;
        public ScalerComplexHandler[] Linear;
        public ScalerComplexHandler[] Random;
    }

    private static final class ScalerSimpleBlock {
        public String name;
        public int gfxFlags;
        public int xscale, yscale;
        public ScalerLineHandler[][] Linear;
        public ScalerLineHandler[][] Random;
    }


    private static final int SCALE_LEFT = 0x1;
    private static final int SCALE_RIGHT = 0x2;
    private static final int SCALE_FULL = 0x4;

    ScalerSimpleBlock ScaleNormal1x;
    ScalerSimpleBlock ScaleNormal2x;
    // #if RENDER_USE_ADVANCED_SCALERS>0
    // extern ScalerSimpleBlock_t ScaleTV2x;
    // extern ScalerSimpleBlock_t ScaleTV3x;
    // extern ScalerSimpleBlock_t ScaleRGB2x;
    // extern ScalerSimpleBlock_t ScaleRGB3x;
    // extern ScalerSimpleBlock_t ScaleScan2x;
    // extern ScalerSimpleBlock_t ScaleScan3x;
    // #endif
    /* Complex scalers */
    // #if RENDER_USE_ADVANCED_SCALERS>2
    // extern ScalerComplexBlock_t ScaleHQ2x;
    // extern ScalerComplexBlock_t ScaleHQ3x;
    // extern ScalerComplexBlock_t Scale2xSaI;
    // extern ScalerComplexBlock_t ScaleSuper2xSaI;
    // extern ScalerComplexBlock_t ScaleSuperEagle;
    // extern ScalerComplexBlock_t ScaleAdvMame2x;
    // extern ScalerComplexBlock_t ScaleAdvMame3x;
    // extern ScalerComplexBlock_t ScaleAdvInterp2x;
    // extern ScalerComplexBlock_t ScaleAdvInterp3x;
    // #endif
    // #if RENDER_USE_ADVANCED_SCALERS>1
    // extern ScalerLineBlock_t ScalerCache;
    // #endif


    byte[] Scaler_Aspect = new byte[SCALER_MAXHEIGHT];
    short[] Scaler_ChangedLines = new short[SCALER_MAXHEIGHT];
    int Scaler_ChangedLineIndex;

    // 8, 16, 32 bit 범위를 모두 포괄하기 위해 가장 큰 자료구조를 기준으로 생성
    int[] scalerWriteCache = new int[4 * SCALER_MAXWIDTH * 3];

    private void ScalerAddLines(int changed, int count) {
        if ((Scaler_ChangedLineIndex & 1) == changed) {
            Scaler_ChangedLines[Scaler_ChangedLineIndex] += (short) count;
        } else {
            Scaler_ChangedLines[++Scaler_ChangedLineIndex] = (short) count;
        }
        _scaleOutWriteIndex += _scaleOutPitch * count;
    }

    // -- #region Normal1x

    // private void Normal1x_8_32_L(byte[] src, uint index)
    private void Normal1x_8_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        byte S = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                // PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    //// const PTYPE P = PMAKE(S);
                    // Color P = pal.lut[S];
                    P = _pal.lut[S];
                    //// SCALERFUNC;
                    // line0[line0idx] = P;
                    line0[line0idx] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = 1;
        ScalerAddLines(hadChange, scaleLines);
    }

    // private void Normal1x_9_32_L(byte[] src, uint index)
    private void Normal1x_9_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        byte S = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            // 있는 단위가 32비트라서?
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]
                    && (_pal.modified[src[sidx]] | _pal.modified[src[sidx + 1]]
                            | _pal.modified[src[sidx + 2]] | _pal.modified[src[sidx + 3]]) == 0) {
                x -= 4;
                sidx += 4;
                cidx += 4;
                // line0+=4*SCALERWIDTH;
                line0idx += 4;

            } else {

                // PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    //// const PTYPE P = PMAKE(S);
                    // Color P = pal.lut[S];
                    P = _pal.lut[S];
                    //// SCALERFUNC;
                    // line0[line0idx] = P;
                    line0[line0idx] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = 1;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_15_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int srcVal = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                //// PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    // S.B0 = src[sidx]; S.B1 = src[sidx + 1];
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    line0[line0idx] = ((srcVal & (0x1F << 10)) << 9) | ((srcVal & (0x1F << 5)) << 6)
                            | ((srcVal & 0x1F) << 3);

                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = 1;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_16_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int srcVal = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                //// PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    line0[line0idx] = ((srcVal & (0x1F << 11)) << 8) | ((srcVal & (0x3F << 5)) << 5)
                            | ((srcVal & 31) << 3);
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = 1;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_32_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int S = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                //// PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = ByteConv.getInt(src, sidx);
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    line0[line0idx] = S;
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = 1;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_8_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        byte S = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                // PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    // const PTYPE P = PMAKE(S);
                    P = _pal.lut[S];
                    // SCALERFUNC;
                    line0[line0idx] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * (1 - 1),
                    _scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * 1, _srcWidth * 1);
        }
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_9_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        byte S = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]
                    && (_pal.modified[src[sidx]] | _pal.modified[src[sidx + 1]]
                            | _pal.modified[src[sidx + 2]] | _pal.modified[src[sidx + 3]]) == 0) {
                x -= 4;
                sidx += 4;
                cidx += 4;
                // line0+=4*SCALERWIDTH;
                line0idx += 4;

            } else {

                // PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    // const PTYPE P = PMAKE(S);
                    P = _pal.lut[S];
                    // SCALERFUNC;
                    line0[line0idx] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * (1 - 1),
                    _scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * 1, _srcWidth * 1);
        }
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_15_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int srcVal = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                //// PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    line0[line0idx] = ((srcVal & (0x1F << 10)) << 9) | ((srcVal & (0x1F << 5)) << 6)
                            | ((srcVal & 0x1F) << 3);
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * (1 - 1),
                    _scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * 1, _srcWidth * 1);
        }
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_16_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int srcVal = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                //// PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // uint line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    line0[line0idx] = ((srcVal & (0x1F << 11)) << 8) | ((srcVal & (0x3F << 5)) << 5)
                            | ((srcVal & 31) << 3);
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * (1 - 1),
                    _scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * 1, _srcWidth * 1);
        }
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal1x_32_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit;
            } else {


                //// PTYPE *line1 = (PTYPE *)(((Bit8u*)line0)+ scale_outPitch);
                // int line1idx = line0idx + scale_outPitch;

                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    line0[line0idx] = ByteConv.getInt(src, sidx);
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * (1 - 1),
                    _scaleOutWrite, _scaleOutWriteIndex + _scaleOutPitch * 1, _srcWidth * 1);
        }
        ScalerAddLines(hadChange, scaleLines);
    }

    // -- #endregion

    // -- #region Normal2x

    // Normal2x_8_32_R을 기준으로 수정해야함 :
    private void Normal2x_8_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        // 원래는 출력buffer가 byte배열로 정의 되었을 경우 1라인당 byte갯수였으나
        // 출력buffer를 uint(4bytes)로 고정했기 때문에(기본 단위를 uint로 했기 때문에) uint갯수를 기준으로 함
        int PixelsPerScaleOutLine = _scaleOutPitch;
        int scalerwidth = 2;
        int scalerheight = 2;
        Byte S = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            // 있는 단위가 32비트라서?
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = scalerWriteCache;
                int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    // const PTYPE P = PMAKE(S);
                    P = _pal.lut[S];
                    // SCALERFUNC;
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                // defined(SCALERLINEAR)
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = scalerheight;
        ScalerAddLines(hadChange, scaleLines);
    }

    // TODO Normal2x_9_32_R기준으로 수정할 것
    private void Normal2x_9_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int scaleOutLinePixels = _scaleOutPitch / 4;// render.scale.outPitch를 4바이트 단위로 조정
        int scalerwidth = 2;
        int scalerheight = 2;
        byte S = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]
                    && (_pal.modified[src[sidx]] | _pal.modified[src[sidx + 1]]
                            | _pal.modified[src[sidx + 2]] | _pal.modified[src[sidx + 3]]) == 0) {
                x -= 4;
                sidx += 4;
                cidx += 4;
                // line0+=4*SCALERWIDTH;
                line0idx += 4;

            } else {
                int[] line1 = scalerWriteCache;
                int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    // const PTYPE P = PMAKE(S);
                    P = _pal.lut[S];
                    // SCALERFUNC;
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                // defined(SCALERLINEAR)
                int copyLen = line0idx;
                ArrayHelper.copy(line1, 0, line0, line0idx - copyLen + scaleOutLinePixels, copyLen);
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = scalerheight;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_15_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int scaleOutLinePixels = _scaleOutPitch / 4;// render.scale.outPitch를 4바이트 단위로 조정
        int scalerwidth = 2;
        int scalerheight = 2;
        int srcVal = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = scalerWriteCache;
                int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;

                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    P = ((srcVal & (0x1F << 10)) << 9) | ((srcVal & (0x1F << 5)) << 6)
                            | ((srcVal & 0x1F) << 3);
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                // defined(SCALERLINEAR)
                int copyLen = line0idx;
                ArrayHelper.copy(line1, 0, line0, line0idx - copyLen + scaleOutLinePixels, copyLen);
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = scalerheight;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_16_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int scaleOutLinePixels = _scaleOutPitch / 4;// render.scale.outPitch를 4바이트 단위로 조정
        int scalerwidth = 2;
        int scalerheight = 2;
        int srcVal = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = scalerWriteCache;
                int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                int P = 0;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    P = ((srcVal & (0x1F << 11)) << 8) | ((srcVal & (0x3F << 5)) << 5)
                            | ((srcVal & 31) << 3);

                    // SCALERFUNC;
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                // defined(SCALERLINEAR)
                int copyLen = line0idx;
                ArrayHelper.copy(line1, 0, line0, line0idx - copyLen + scaleOutLinePixels, copyLen);
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = scalerheight;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_32_32_L(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int scaleOutLinePixels = _scaleOutPitch / 4;// render.scale.outPitch를 4바이트 단위로 조정
        int scalerwidth = 2;
        int scalerheight = 2;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = scalerWriteCache;
                int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    P = ByteConv.getInt(src, sidx);;
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    // const PTYPE P = PMAKE(S);

                    // SCALERFUNC;
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                // defined(SCALERLINEAR)
                int copyLen = line0idx;
                ArrayHelper.copy(line1, 0, line0, line0idx - copyLen + scaleOutLinePixels, copyLen);
            }
        }

        // Bitu scaleLines = SCALERHEIGHT;
        int scaleLines = scalerheight;
        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_8_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        // 원래는 출력buffer가 byte배열로 정의 되었을 경우 1라인당 byte갯수였으나
        // 출력buffer를 uint(4bytes)로 고정했기 때문에(기본 단위를 uint로 했기 때문에) uint갯수를 기준으로 함
        int PixelsPerScaleOutLine = _scaleOutPitch;
        int scalerwidth = 2;
        int scalerheight = 2;
        byte S = 0;
        int P = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = line0;
                int line1idx = line0idx + PixelsPerScaleOutLine;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    // const PTYPE P = PMAKE(S);
                    P = _pal.lut[S];
                    // SCALERFUNC;
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                //// defined(SCALERLINEAR)
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - scalerheight) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite,
                    _scaleOutWriteIndex + PixelsPerScaleOutLine * (scalerheight - 1),
                    _scaleOutWrite, _scaleOutWriteIndex + PixelsPerScaleOutLine * scalerheight,
                    _srcWidth * scalerheight);
        }

        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_9_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        // 원래는 출력buffer가 byte배열로 정의 되었을 경우 1라인당 byte갯수였으나
        // 출력buffer를 uint(4bytes)로 고정했기 때문에(기본 단위를 uint로 했기 때문에) uint갯수를 기준으로 함
        int PixelsPerScaleOutLine = _scaleOutPitch;
        int scalerwidth = 2;
        int scalerheight = 2;
        int P = 0;
        byte S = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]
                    && (_pal.modified[src[sidx]] | _pal.modified[src[sidx + 1]]
                            | _pal.modified[src[sidx + 2]] | _pal.modified[src[sidx + 3]]) == 0) {
                x -= 4;
                sidx += 4;
                cidx += 4;
                // line0+=4*SCALERWIDTH;
                line0idx += 4 * scalerwidth;
            } else {
                int[] line1 = line0;
                int line1idx = line0idx + PixelsPerScaleOutLine;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    S = src[sidx];
                    cache[cidx] = S;
                    sidx++;
                    cidx++;
                    // const PTYPE P = PMAKE(S);
                    P = _pal.lut[S];
                    // SCALERFUNC;
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                // defined(SCALERLINEAR)
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - scalerheight) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite,
                    _scaleOutWriteIndex + PixelsPerScaleOutLine * (scalerheight - 1),
                    _scaleOutWrite, _scaleOutWriteIndex + PixelsPerScaleOutLine * scalerheight,
                    _srcWidth * scalerheight);
        }

        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_15_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int scaleOutLinePixels = _scaleOutPitch / 4;// render.scale.outPitch를 4바이트 단위로 조정
        int scalerwidth = 2;
        int scalerheight = 2;
        int srcVal = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = scalerWriteCache;
                // int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    line0[line0idx] = ((srcVal & (0x1F << 10)) << 9) | ((srcVal & (0x1F << 5)) << 6)
                            | ((srcVal & 0x1F) << 3);
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
                int copyLen = line0idx;
                // BituMove(((Bit8u*)line0) - copyLen + render.scale.outPitch, WC[0], copyLen);
                // render.scale.outPitch를 4바이트 단위로 조정 => scaleOutLinePixels
                ArrayHelper.copy(line1, 0, line0, line0idx - copyLen + scaleOutLinePixels, copyLen);
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite,
                    _scaleOutWriteIndex + scaleOutLinePixels * (scalerheight - 1), _scaleOutWrite,
                    _scaleOutWriteIndex + scalerheight, _srcWidth * scalerheight);
        }

        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_16_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int scaleOutLinePixels = _scaleOutPitch / 4;// render.scale.outPitch를 4바이트 단위로 조정
        int scalerwidth = 2;
        int scalerheight = 2;
        int srcVal = 0;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = scalerWriteCache;
                // int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    srcVal = ByteConv.getShort(src, sidx);
                    cache[cidx] = src[sidx];
                    cache[cidx + 1] = src[sidx + 1];
                    sidx += 2;
                    cidx += 2;
                    // const PTYPE P = PMAKE(S);
                    // SCALERFUNC;
                    line0[line0idx] = ((srcVal & (0x1F << 11)) << 8) | ((srcVal & (0x3F << 5)) << 5)
                            | ((srcVal & 31) << 3);
                    // line0idx += SCALERWIDTH;
                    line0idx += 1;
                    // line1idx += SCALERWIDTH;
                    // line1idx += 1;
                }
                // defined(SCALERLINEAR)
                int copyLen = line0idx;
                // BituMove(((Bit8u*)line0) - copyLen + render.scale.outPitch, WC[0], copyLen);
                // render.scale.outPitch를 4바이트 단위로 조정 => scaleOutLinePixels
                ArrayHelper.copy(line1, 0, line0, line0idx - copyLen + scaleOutLinePixels, copyLen);
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite,
                    _scaleOutWriteIndex + scaleOutLinePixels * (scalerheight - 1), _scaleOutWrite,
                    _scaleOutWriteIndex + scalerheight, _srcWidth * scalerheight);
        }

        ScalerAddLines(hadChange, scaleLines);
    }

    private void Normal2x_32_32_R(byte[] src, int index) {
        // 존재하지 않음 SCALERLINEAR

        /* Clear the complete line marker */
        int hadChange = 0;
        // const SRCTYPE *src = (SRCTYPE*)s;
        int sidx = index;
        byte[] cache = _scaleCacheRead;
        int cidx = _scaleCacheReadIndex;
        _scaleCacheReadIndex += _scaleCachePitch;
        int[] line0 = _scaleOutWrite;// 디스플레이 장치의 픽셀 해상도에 따라 처리, 원래는 32bit 단위이나 color로 변경
        int line0idx = _scaleOutWriteIndex;
        int sizeOfUnit = SIZE_UINT / SIZE_UBYTE;
        int scaleOutLinePixels = _scaleOutPitch / 4;// render.scale.outPitch를 4바이트 단위로 조정
        int scalerwidth = 2;
        int scalerheight = 2;
        for (int x = _srcWidth; x > 0;) {
            if (src[sidx] == cache[cidx] && src[sidx + 1] == cache[cidx + 1]
                    && src[sidx + 2] == cache[cidx + 2] && src[sidx + 3] == cache[cidx + 3]) {
                x -= sizeOfUnit;
                sidx += sizeOfUnit;
                cidx += sizeOfUnit;
                // line0idx+=(SIZE_UINT/sizeof(SRCTYPE))*SCALERWIDTH;
                line0idx += sizeOfUnit * scalerwidth;
            } else {
                int[] line1 = scalerWriteCache;
                int line1idx = 0;
                // defined(SCALERLINEAR)
                hadChange = 1;
                int P = 0;
                for (int i = x > 32 ? 32 : x; i > 0; i--, x--) {
                    P = ByteConv.getInt(src, sidx);
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    cache[cidx++] = src[sidx++];
                    // const PTYPE P = PMAKE(S);

                    // SCALERFUNC;
                    line0[line0idx] = P;
                    line0[line0idx + 1] = P;
                    line1[line1idx] = P;
                    line1[line1idx + 1] = P;
                    // line0idx += SCALERWIDTH;
                    line0idx += scalerwidth;
                    // line1idx += SCALERWIDTH;
                    line1idx += scalerwidth;
                }
                // defined(SCALERLINEAR)
                int copyLen = line0idx;
                ArrayHelper.copy(line1, 0, line0, line0idx - copyLen + scaleOutLinePixels, copyLen);
            }
        }

        int scaleLines = Scaler_Aspect[_scaleOutLine++];
        // if ( scaleLines - SCALERHEIGHT && hadChange ) {

        if ((scaleLines - 1) != 0 && hadChange != 0) {
            ArrayHelper.copy(_scaleOutWrite,
                    _scaleOutWriteIndex + scaleOutLinePixels * (scalerheight - 1), _scaleOutWrite,
                    _scaleOutWriteIndex + scalerheight, _srcWidth * scalerheight);
        }

        ScalerAddLines(hadChange, scaleLines);
    }

    // -- #endregion

    /*--------------------------- end Render_Scalers -----------------------------*/
    /*--------------------------- begin Render_t -----------------------------*/
    public final class RenderPal {
        public Color[] rgb = new Color[256];
        public int[] lut = new int[256];
        public byte[][] lutBytes = new byte[256][];
        public boolean changed;
        public byte[] modified = new byte[256];
        public int first;
        public int last;

        public RenderPal() {
            for (int i = 0; i < rgb.length; i++) {
                rgb[i] = new Color(0, 0, 0, 0);
            }
        }
    }

    // src
    int _srcWidth /* 소스의 가로 픽셀 */, _srcStart;
    int _srcHeight;// 소스의 세로 픽셀
    int _srcBpp;
    boolean _srcDblw, _srcDblh;
    double _srcRatio;
    float _srcFps;
    // frameskip
    int _frameSkipCount;
    int _frameSkipMax;
    int _frameSkipIndex;
    byte[] _frameSkipHadSkip = new byte[RENDER_SKIP_CACHE];
    // scale;
    int _scaleSize;
    int _scaleInMode;
    int _scaleOutMode;
    scalerOperation _scaleOp;
    boolean _scaleClearCache;
    boolean _scaleForced;
    ScalerLineHandler _scaleLineHandler;
    ScalerLineHandler _scaleLinePalHandler;
    ScalerComplexHandler _scaleComplexHandler;
    int _scaleBlocks, _scaleLastBlock;
    int _scaleOutPitch;// 원래는 출력쪽 한줄당 byte수
    int[] _scaleOutWrite;// 픽셀당 32비트로 고정
    int _scaleOutWriteIndex;// uint
    int _scaleCachePitch;// 한줄당 byte 수
    byte[] _scaleCacheRead;
    int _scaleCacheReadIndex;// uint
    int _scaleInHeight, _scaleInLine, _scaleOutLine;
    RenderPal _pal = new RenderPal();
    boolean _updating;
    boolean _active;
    boolean _aspect;
    boolean _fullFrame;
    /*--------------------------- end Render_t -----------------------------*/

}
