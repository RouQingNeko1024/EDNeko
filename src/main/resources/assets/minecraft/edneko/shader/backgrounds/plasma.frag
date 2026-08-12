#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv.x *= iResolution.x / iResolution.y;
    
    float t = iTime * 0.3;
    
    vec3 color = vec3(0.0);
    
    for(float i = 0.0; i < 4.0; i++) {
        float scale = 1.0 + i * 0.5;
        vec2 p = uv * scale + vec2(t * 0.1, t * 0.05) * (i + 1.0);
        
        float n = noise(p);
        n += 0.5 * noise(p * 2.0);
        n += 0.25 * noise(p * 4.0);
        n /= 1.75;
        
        float plasma = sin(n * 10.0 + t + i);
        plasma = plasma * 0.5 + 0.5;
        
        vec3 col1 = vec3(0.5, 0.0, 0.5);
        vec3 col2 = vec3(0.0, 0.5, 1.0);
        vec3 col3 = vec3(1.0, 0.3, 0.0);
        
        vec3 plasmaColor = mix(col1, col2, plasma);
        plasmaColor = mix(plasmaColor, col3, sin(n * 5.0 + t * 2.0) * 0.5 + 0.5);
        
        color += plasmaColor * 0.25;
    }
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
