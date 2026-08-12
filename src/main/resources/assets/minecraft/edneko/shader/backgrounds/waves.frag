#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv.x *= iResolution.x / iResolution.y;
    
    vec3 color = vec3(0.0);
    
    for(float i = 0.0; i < 5.0; i++) {
        float wave = sin(uv.x * 3.0 + iTime + i * 0.5) * 0.1;
        wave += sin(uv.x * 5.0 - iTime * 0.7 + i * 0.3) * 0.05;
        wave += sin(uv.x * 8.0 + iTime * 1.3 + i * 0.7) * 0.025;
        
        float y = 0.3 + i * 0.15 + wave;
        float dist = abs(uv.y - y);
        
        float waveLine = smoothstep(0.02, 0.0, dist);
        
        vec3 waveColor = vec3(
            0.3 + 0.7 * sin(iTime + i),
            0.3 + 0.7 * sin(iTime + i + 2.0),
            0.3 + 0.7 * sin(iTime + i + 4.0)
        );
        
        color += waveColor * waveLine * (1.0 - i * 0.15);
    }
    
    vec3 bgColor = vec3(0.05, 0.05, 0.15);
    color = bgColor + color;
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
