#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

vec2 complexMul(vec2 a, vec2 b) {
    return vec2(a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv = uv * 2.0 - 1.0;
    uv.x *= iResolution.x / iResolution.y;
    
    vec2 z = uv * 2.0;
    vec2 c = vec2(-0.7 + sin(iTime * 0.1) * 0.1, 0.27 + cos(iTime * 0.15) * 0.05);
    
    float iter = 0.0;
    const float maxIter = 50.0;
    
    for(float i = 0.0; i < maxIter; i++) {
        z = complexMul(z, z) + c;
        if(length(z) > 2.0) break;
        iter++;
    }
    
    float colorVal = iter / maxIter;
    colorVal = pow(colorVal, 0.5);
    
    vec3 color1 = vec3(0.0, 0.0, 0.1);
    vec3 color2 = vec3(0.0, 0.5, 0.8);
    vec3 color3 = vec3(1.0, 0.5, 0.0);
    vec3 color4 = vec3(1.0, 1.0, 1.0);
    
    vec3 color;
    if(colorVal < 0.33) {
        color = mix(color1, color2, colorVal * 3.0);
    } else if(colorVal < 0.66) {
        color = mix(color2, color3, (colorVal - 0.33) * 3.0);
    } else {
        color = mix(color3, color4, (colorVal - 0.66) * 3.0);
    }
    
    if(iter >= maxIter - 1.0) {
        color = vec3(0.0);
    }
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
