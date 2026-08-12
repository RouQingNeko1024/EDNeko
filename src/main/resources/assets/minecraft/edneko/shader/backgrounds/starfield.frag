#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv.x *= iResolution.x / iResolution.y;
    
    vec3 color = vec3(0.0);
    
    float stars = 0.0;
    for(float i = 0.0; i < 3.0; i++) {
        float scale = 1.0 + i * 2.0;
        vec2 p = uv * scale;
        vec2 id = floor(p);
        vec2 gv = fract(p) - 0.5;
        
        float n = hash(id);
        float size = fract(n * 123.456) * 0.03 + 0.005;
        
        float star = length(gv) - size;
        star = smoothstep(0.0, 0.01, star);
        star = 1.0 - star;
        
        float twinkle = sin(iTime * (2.0 + n * 3.0) + n * 6.28) * 0.5 + 0.5;
        star *= 0.5 + 0.5 * twinkle;
        
        stars += star * (1.0 - i * 0.3);
    }
    
    vec3 skyColor = mix(vec3(0.02, 0.02, 0.05), vec3(0.05, 0.02, 0.1), uv.y);
    color = skyColor + vec3(stars);
    
    float nebula = hash(uv * 3.0 + iTime * 0.02) * 0.1;
    color += vec3(0.1, 0.0, 0.2) * nebula * (1.0 - uv.y);
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
