#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    
    float t = iTime * 0.2;
    
    vec3 color1 = vec3(0.1, 0.2, 0.5);
    vec3 color2 = vec3(0.5, 0.1, 0.5);
    vec3 color3 = vec3(0.1, 0.5, 0.3);
    
    float wave1 = sin(uv.x * 3.0 + t) * 0.5 + 0.5;
    float wave2 = sin(uv.y * 2.0 + t * 1.3) * 0.5 + 0.5;
    float wave3 = sin((uv.x + uv.y) * 1.5 + t * 0.7) * 0.5 + 0.5;
    
    vec3 color = mix(color1, color2, wave1);
    color = mix(color, color3, wave2 * wave3);
    
    float vignette = 1.0 - length(uv - 0.5) * 0.8;
    color *= vignette;
    
    float pulse = sin(t * 2.0) * 0.1 + 0.9;
    color *= pulse;
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
