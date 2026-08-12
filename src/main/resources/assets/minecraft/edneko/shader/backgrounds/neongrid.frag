#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv.x *= iResolution.x / iResolution.y;
    
    vec3 color = vec3(0.02, 0.02, 0.05);
    
    float gridSize = 0.1;
    vec2 grid = fract(uv / gridSize) - 0.5;
    vec2 id = floor(uv / gridSize);
    
    float lineX = smoothstep(0.02, 0.01, abs(grid.x));
    float lineY = smoothstep(0.02, 0.01, abs(grid.y));
    float lines = max(lineX, lineY);
    
    float pulse = sin(iTime * 2.0 + id.x * 0.5 + id.y * 0.5) * 0.5 + 0.5;
    
    vec3 neonColor = vec3(0.0, 0.8, 1.0) * pulse;
    neonColor += vec3(1.0, 0.0, 0.8) * (1.0 - pulse);
    
    color += neonColor * lines * 0.5;
    
    float glow = 0.0;
    for(float i = 0.0; i < 3.0; i++) {
        vec2 p = vec2(
            sin(iTime * 0.5 + i * 2.0) * 0.5 + 0.5,
            cos(iTime * 0.3 + i * 1.5) * 0.5 + 0.5
        );
        p *= iResolution.x / iResolution.y;
        
        float dist = length(uv - p);
        glow += 0.01 / (dist + 0.01) * 0.1;
    }
    
    color += vec3(0.0, 0.5, 1.0) * glow;
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
