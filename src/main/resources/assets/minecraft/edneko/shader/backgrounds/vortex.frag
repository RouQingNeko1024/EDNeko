#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    uv = uv * 2.0 - 1.0;
    uv.x *= iResolution.x / iResolution.y;
    
    float angle = atan(uv.y, uv.x);
    float radius = length(uv);
    
    float spiral = sin(angle * 5.0 - radius * 10.0 + iTime * 3.0);
    spiral = spiral * 0.5 + 0.5;
    
    float vortex = 1.0 / (radius + 0.5);
    vortex *= spiral;
    
    vec3 color1 = vec3(0.1, 0.0, 0.3);
    vec3 color2 = vec3(0.0, 0.3, 0.5);
    vec3 color3 = vec3(0.5, 0.0, 0.5);
    
    vec3 color = mix(color1, color2, spiral);
    color = mix(color, color3, vortex);
    
    color *= vortex * 2.0;
    
    float glow = exp(-radius * 2.0);
    color += vec3(0.5, 0.3, 0.8) * glow * 0.5;
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
