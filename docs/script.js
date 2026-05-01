// Canvas粒子背景系统
class ParticleSystem {
  constructor() {
    this.canvas = document.getElementById('particle-bg');
    this.ctx = this.canvas.getContext('2d');
    this.particles = [];
    this.mouse = { x: null, y: null };
    this.particleCount = 80;
    this.connectionDistance = 150;
    this.mouseDistance = 200;
    
    this.init();
    this.bindEvents();
    this.animate();
  }
  
  init() {
    this.resize();
    this.createParticles();
  }
  
  resize() {
    this.canvas.width = window.innerWidth;
    this.canvas.height = window.innerHeight;
  }
  
  createParticles() {
    this.particles = [];
    for (let i = 0; i < this.particleCount; i++) {
      this.particles.push({
        x: Math.random() * this.canvas.width,
        y: Math.random() * this.canvas.height,
        vx: (Math.random() - 0.5) * 0.5,
        vy: (Math.random() - 0.5) * 0.5,
        size: Math.random() * 2 + 1
      });
    }
  }
  
  bindEvents() {
    window.addEventListener('resize', () => this.resize());
    
    window.addEventListener('mousemove', (e) => {
      this.mouse.x = e.clientX;
      this.mouse.y = e.clientY;
    });
    
    window.addEventListener('mouseleave', () => {
      this.mouse.x = null;
      this.mouse.y = null;
    });
  }
  
  drawParticle(particle) {
    this.ctx.beginPath();
    this.ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2);
    this.ctx.fillStyle = 'rgba(90, 156, 240, 0.5)';
    this.ctx.fill();
  }
  
  drawConnection(p1, p2) {
    const dx = p1.x - p2.x;
    const dy = p1.y - p2.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    if (distance < this.connectionDistance) {
      const opacity = (1 - distance / this.connectionDistance) * 0.3;
      this.ctx.beginPath();
      this.ctx.moveTo(p1.x, p1.y);
      this.ctx.lineTo(p2.x, p2.y);
      this.ctx.strokeStyle = `rgba(90, 156, 240, ${opacity})`;
      this.ctx.lineWidth = 1;
      this.ctx.stroke();
    }
  }
  
  drawMouseConnection(particle) {
    if (this.mouse.x === null || this.mouse.y === null) return;
    
    const dx = particle.x - this.mouse.x;
    const dy = particle.y - this.mouse.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    if (distance < this.mouseDistance) {
      const opacity = (1 - distance / this.mouseDistance) * 0.5;
      this.ctx.beginPath();
      this.ctx.moveTo(particle.x, particle.y);
      this.ctx.lineTo(this.mouse.x, this.mouse.y);
      this.ctx.strokeStyle = `rgba(90, 156, 240, ${opacity})`;
      this.ctx.lineWidth = 1.5;
      this.ctx.stroke();
    }
  }
  
  updateParticle(particle) {
    particle.x += particle.vx;
    particle.y += particle.vy;
    
    // 边界检测
    if (particle.x < 0 || particle.x > this.canvas.width) {
      particle.vx *= -1;
    }
    if (particle.y < 0 || particle.y > this.canvas.height) {
      particle.vy *= -1;
    }
  }
  
  animate() {
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    
    // 绘制粒子和连接
    for (let i = 0; i < this.particles.length; i++) {
      const particle = this.particles[i];
      this.updateParticle(particle);
      this.drawParticle(particle);
      
      // 粒子间连接
      for (let j = i + 1; j < this.particles.length; j++) {
        this.drawConnection(particle, this.particles[j]);
      }
      
      // 鼠标连接
      this.drawMouseConnection(particle);
    }
    
    requestAnimationFrame(() => this.animate());
  }
}

// Element Plus Carousel Vue 应用
document.addEventListener('DOMContentLoaded', () => {
  const { createApp } = Vue;
  
  const carouselApp = createApp({
    data() {
      return {
        screenshots: [
          {
            image: 'pic/en/20260406/mainpage.png',
            title: '主界面',
            caption: '主界面 - 简洁的作业编辑区'
          },
          {
            image: 'pic/en/historical-homework-search.png',
            title: '历史查询',
            caption: '历史作业查询 - 快速检索过往记录'
          },
          {
            image: 'pic/en/20260406/settings.png',
            title: '设置界面',
            caption: '设置界面 - 个性化配置中心'
          },
          {
            image: 'pic/en/20260406/lang.png',
            title: '语言设置',
            caption: '多语言支持 - 11种语言可选'
          }
        ]
      };
    }
  });
  
  carouselApp.use(ElementPlus);
  carouselApp.mount('#carousel-app');
});

// Anime.js动画
document.addEventListener('DOMContentLoaded', () => {
  // 初始化粒子背景
  new ParticleSystem();
  
  // Hero区域入场动画
  anime.timeline({
    easing: 'easeOutExpo',
    duration: 1000
  })
  .add({
    targets: '.hero-title',
    opacity: [0, 1],
    translateY: [50, 0],
    delay: 200
  })
  .add({
    targets: '.hero-subtitle',
    opacity: [0, 1],
    translateY: [30, 0],
    offset: '-600'
  })
  .add({
    targets: '.feature-tag',
    opacity: [0, 1],
    translateY: [20, 0],
    delay: anime.stagger(100),
    offset: '-400'
  })
  .add({
    targets: '.hero-buttons',
    opacity: [0, 1],
    translateY: [20, 0],
    offset: '-200'
  });
  
  // 滚动触发动画
  const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -100px 0px'
  };
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const element = entry.target;
        
        // 功能卡片动画
        if (element.classList.contains('feature-card')) {
          anime({
            targets: element,
            opacity: [0, 1],
            scale: [0.9, 1],
            translateY: [30, 0],
            duration: 800,
            easing: 'easeOutCubic'
          });
          observer.unobserve(element);
        }
        
        // 下载卡片动画
        if (element.classList.contains('download-card')) {
          anime({
            targets: element,
            opacity: [0, 1],
            translateY: [40, 0],
            duration: 1000,
            easing: 'easeOutCubic'
          });
          observer.unobserve(element);
        }
        
        // 技术卡片动画
        if (element.classList.contains('tech-card')) {
          anime({
            targets: element,
            opacity: [0, 1],
            translateX: [-30, 0],
            duration: 800,
            delay: anime.stagger(100),
            easing: 'easeOutCubic'
          });
          observer.unobserve(element);
        }
      }
    });
  }, observerOptions);
  
  // 观察所有需要动画的元素
  document.querySelectorAll('.feature-card, .download-card, .tech-card').forEach(el => {
    el.style.opacity = '0';
    observer.observe(el);
  });
  
  // 按钮悬停动画
  document.querySelectorAll('.btn').forEach(btn => {
    btn.addEventListener('mouseenter', () => {
      anime({
        targets: btn,
        scale: 1.05,
        duration: 200,
        easing: 'easeOutCubic'
      });
    });
    
    btn.addEventListener('mouseleave', () => {
      anime({
        targets: btn,
        scale: 1,
        duration: 200,
        easing: 'easeOutCubic'
      });
    });
  });
  
  // 功能卡片悬停光晕效果
  document.querySelectorAll('.feature-card').forEach(card => {
    card.addEventListener('mouseenter', () => {
      anime({
        targets: card.querySelector('.card-icon'),
        scale: 1.1,
        duration: 300,
        easing: 'easeOutCubic'
      });
    });
    
    card.addEventListener('mouseleave', () => {
      anime({
        targets: card.querySelector('.card-icon'),
        scale: 1,
        duration: 300,
        easing: 'easeOutCubic'
      });
    });
  });
  
  // 导航栏滚动效果
  let lastScroll = 0;
  const nav = document.querySelector('.glass-nav');
  
  window.addEventListener('scroll', () => {
    const currentScroll = window.pageYOffset;
    
    if (currentScroll > 100) {
      nav.style.background = 'rgba(30, 30, 30, 0.95)';
    } else {
      nav.style.background = 'rgba(30, 30, 30, 0.8)';
    }
    
    lastScroll = currentScroll;
  });
});

// 平滑滚动增强
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
  anchor.addEventListener('click', function(e) {
    e.preventDefault();
    const target = document.querySelector(this.getAttribute('href'));
    if (target) {
      const offsetTop = target.offsetTop - 80;
      window.scrollTo({
        top: offsetTop,
        behavior: 'smooth'
      });
    }
  });
});
