// Copyright 2010-2024 JogAmp Community. All rights reserved.

#ifndef uniforms_glsl
#define uniforms_glsl

uniform mat4    gcu_PMVMatrix01[3]; // P, Mv, and Mvi
uniform vec4    gcu_ColorStatic;
uniform float   gcu_Weight;

#ifdef USE_COLOR_TEXTURE
    uniform vec2  gcu_ColorTexBBox[3]; // box-min[2], box-max[2] and tex-size[2] 
#endif
#ifdef USE_AABBOX_CLIPPING
    uniform vec3  gcu_ClipBBox[2]; // box-min[3], box-max[3]
#endif    

uniform mat4    gcu_PMVMatrix02[3]; // P, Mv, and Mvi
uniform sampler2D  gcu_FboTexUnit;

/** 
 * .x .y : texture-, fbo- or screen-size
 */
uniform vec2   gcu_FboTexSize;

#endif // uniforms_glsl
