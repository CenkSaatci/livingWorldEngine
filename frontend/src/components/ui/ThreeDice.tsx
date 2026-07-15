import { useEffect, useRef } from 'react';
import * as THREE from 'three';

interface ThreeDiceResult {
  value: number;
}

interface Props {
  results: ThreeDiceResult[];
  modifier: number;
  total: number;
}

/**
 * 3D-Würfel mit three.js.
 * Mehrere Würfel als Box-Geometrien mit animierter Rotation.
 * Nach ~1.5s landen sie auf dem Ergebnis und zeigen die Augenzahl als Label.
 */
export function ThreeDice({ results, modifier, total }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const sceneRef = useRef<{
    scene: THREE.Scene;
    camera: THREE.PerspectiveCamera;
    renderer: THREE.WebGLRenderer;
    dice: { mesh: THREE.Mesh; value: number; targetRotation: THREE.Euler }[];
    animId: number;
  } | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    const el = containerRef.current;
    const w = el.clientWidth || 300;
    const h = el.clientHeight || 200;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, w / h, 0.1, 100);
    camera.position.set(0, 2, 6);
    camera.lookAt(0, 0, 0);

    const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    renderer.setSize(w, h);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    el.appendChild(renderer.domElement);

    // Licht
    const ambient = new THREE.AmbientLight(0x404060);
    scene.add(ambient);
    const directional = new THREE.DirectionalLight(0xffffff, 1);
    directional.position.set(2, 5, 3);
    scene.add(directional);

    // Würfel erstellen
    const diceData: { mesh: THREE.Mesh; value: number; targetRotation: THREE.Euler }[] = [];
    const colors = [0x5bb8c5, 0xe0556b, 0x7ac784, 0xf2a65a, 0xa78bfa];

    results.forEach((r, i) => {
      const size = 0.8;
      const geo = new THREE.BoxGeometry(size, size, size);
      const mat = new THREE.MeshStandardMaterial({
        color: colors[i % colors.length],
        roughness: 0.3,
        metalness: 0.1,
      });
      const mesh = new THREE.Mesh(geo, mat);
      const offset = (results.length - 1) * 0.6;
      mesh.position.set(i * 1.2 - offset, 0, 0);
      scene.add(mesh);

      // Zielrotation zufällig basierend auf Wert
      const targetRotation = new THREE.Euler(
        Math.random() * Math.PI * 2,
        Math.random() * Math.PI * 2,
        (r.value * (Math.PI * 2)) / 6,
      );
      mesh.rotation.set(
        Math.random() * Math.PI * 4,
        Math.random() * Math.PI * 4,
        Math.random() * Math.PI * 4,
      );
      diceData.push({ mesh, value: r.value, targetRotation });
    });

    // Animation
    const startTime = Date.now();
    const duration = 1500;

    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      // easeOutCubic — value used implicitly via progress // eslint-disable-line @typescript-eslint/no-unused-vars

      for (const d of diceData) {
        d.mesh.rotation.x += (d.targetRotation.x - d.mesh.rotation.x) * 0.08;
        d.mesh.rotation.y += (d.targetRotation.y - d.mesh.rotation.y) * 0.08;
        d.mesh.rotation.z += (d.targetRotation.z - d.mesh.rotation.z) * 0.08;
      }

      renderer.render(scene, camera);
      if (progress < 1) {
        sceneRef.current!.animId = requestAnimationFrame(animate);
      }
    };

    animate();

    sceneRef.current = { scene, camera, renderer, dice: diceData, animId: 0 };

    return () => {
      if (sceneRef.current) {
        cancelAnimationFrame(sceneRef.current.animId);
        renderer.dispose();
      }
      if (el.contains(renderer.domElement)) el.removeChild(renderer.domElement);
    };
  }, [results]);

  return (
    <div className="flex flex-col items-center gap-3">
      <div ref={containerRef} className="h-48 w-full max-w-xs" />
      <p className="text-2xl font-heading text-text-primary">
        = {total}{' '}
        {modifier !== 0 && (
          <span className="text-accent text-lg">
            ({modifier > 0 ? '+' : ''}
            {modifier})
          </span>
        )}
      </p>
    </div>
  );
}
