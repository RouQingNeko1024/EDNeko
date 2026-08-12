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

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for(int i = 0; i < 5; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv.x *= iResolution.x / iResolution.y;
    
    float t = iTime * 0.1;
    
    vec2 p = uv * 2.0 + vec2(t, t * 0.5);
    
    float n = fbm(p);
    n += 0.5 * fbm(p * 2.0 + t);
    n += 0.25 * fbm(p * 4.0 - t * 0.5);
    
    vec3 skyColor = mix(vec3(0.2, 0.3, 0.5), vec3(0.6, 0.7, 0.9), uv.y);
    
    vec3 cloudColor = vec3(1.0);
    vec3 darkCloud = vec3(0.4, 0.4, 0.5);
    
    float clouds = smoothstep(0.4, 0.8, n);
    vec3 color = mix(skyColor, mix(darkCloud, cloudColor, clouds), clouds);
    
    color = mix(color, skyColor, smoothstep(0.0, 0.3, uv.y) * (1.0 - smoothstep(0.7, 1.0, uv.y)));
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
