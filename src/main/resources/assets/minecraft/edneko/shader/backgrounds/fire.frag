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
    
    vec3 color = vec3(0.0);
    
    float t = iTime * 0.5;
    
    for(float i = 0.0; i < 3.0; i++) {
        float scale = 2.0 + i;
        vec2 p = uv * scale + vec2(t * 0.2, -t * 0.3) * (i + 1.0);
        
        float n = noise(p);
        n += 0.5 * noise(p * 2.0);
        n += 0.25 * noise(p * 4.0);
        
        float flame = pow(n, 2.0);
        flame *= smoothstep(0.0, 0.3, uv.y);
        flame *= 1.0 - smoothstep(0.5, 1.0, uv.y);
        
        vec3 fireColor = mix(
            vec3(1.0, 0.3, 0.0),
            vec3(1.0, 0.8, 0.0),
            flame
        );
        fireColor = mix(fireColor, vec3(0.2, 0.0, 0.0), 1.0 - flame);
        
        color += fireColor * flame * (0.5 - i * 0.1);
    }
    
    color += vec3(0.1, 0.02, 0.0);
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
