// ===== 加载动画控制 =====
(function() {
  var ls = document.getElementById('loading-screen');
  var app = document.getElementById('app');
  var bar = document.getElementById('loader-bar');
  var textEl = document.querySelector('.loader-text');
  var dots = 0;

  var dotsInterval = setInterval(function() {
    dots = (dots + 1) % 4;
    textEl.textContent = 'Loading' + '.'.repeat(dots);
  }, 400);

  anime({
    targets: bar,
    width: '100%',
    duration: 2000,
    easing: 'easeInOutCubic'
  });

  anime({
    targets: textEl,
    opacity: 1,
    duration: 500,
    delay: 300,
    easing: 'easeOutCubic'
  });

  setTimeout(function() {
    clearInterval(dotsInterval);
    anime({
      targets: '.loader-wrapper',
      opacity: 0,
      scale: 0.95,
      duration: 500,
      easing: 'easeOutCubic'
    });
    anime({
      targets: ls,
      opacity: 0,
      duration: 800,
      delay: 300,
      easing: 'easeInCubic',
      complete: function() {
        ls.style.visibility = 'hidden';
        ls.style.pointerEvents = 'none';
        app.style.opacity = '1';
        app.style.visibility = 'visible';
      }
    });
  }, 2500);
})();

// 多语言配置
const i18n = {
  zh: {
    features: '功能特性',
    screenshots: '界面展示',
    download: '下载',
    techStack: '技术栈',
    subtitle: '教室作业展示利器',
    autoSave: '隐式保存',
    historyQuery: '历史查询',
    multiLang: '11种语言',
    downloadNow: '立即下载',
    viewDocs: '查看文档',
    coreFeatures: '核心功能',
    autoSaveTitle: '隐式保存',
    autoSaveDesc: '内容修改自动保存到数据库，SHA256完整性校验防止数据损坏，彻底解决忘记保存问题',
    historyQueryTitle: '历史查询',
    historyQueryDesc: '内置作业数据库，支持按日期快速检索，轻松回溯任意日期的作业记录',
    multiLangTitle: '多语言支持',
    multiLangDesc: '支持11种语言（中英法西阿俄等），适合国际学校和多语言教学环境',
    lockMechanismTitle: '锁定机制',
    lockMechanismDesc: '默认锁定防止误触，60+条可爱提示语，Ctrl+~快捷键快速切换编辑状态',
    screenshotTitle: '一键截屏',
    screenshotDesc: '截图自动保存到剪切板，快速分享作业内容到班级群',
    customSettingsTitle: '个性化设置',
    customSettingsDesc: '自定义字体、字号（0-100px）、初始模板，打造专属的作业展示体验',
    interfaceShowcase: '界面展示',
    githubReleasesDesc: '最新版本及历史版本均可在此获取',
    giteeReleasesDesc: '国内用户推荐使用 Gitee 镜像下载，速度更快',
    goToGithub: '前往 GitHub 下载',
    goToGitee: '前往 Gitee 下载',
    systemRequirements: '系统要求',
    osRequirement: '✅ 操作系统：Windows 10（21H2+）/ 11（仅限 64 位）',
    no32bitNote: '❌ 不支持 32 位系统和 Windows 7 等旧版本',
    cpuRequirement: '🖥️ 处理器：支持 x86-64 的 Intel 或 AMD 处理器',
    diskRequirement: '💾 硬盘：至少 500MB 可用空间',
    macLinuxComing: '🔜 macOS / Linux 版本开发中',
    coreTech: '核心技术',
    devLang: '开发语言',
    uiFramework: 'UI框架',
    buildTool: '构建工具',
    keyDependencies: '关键依赖',
    jsonParse: 'JSON解析',
    httpRequest: 'HTTP请求',
    logging: '日志记录',
    markdownRender: 'Markdown渲染',
    archPattern: '架构模式',
    designPattern: '设计模式',
    businessLogic: '业务逻辑层',
    dataStorage: '数据存储',
    githubRepo: 'GitHub仓库',
    issueFeedback: '问题反馈',
    license: '开源协议：GNU GPL V3 🔓',
    mainInterface: '主界面',
    mainInterfaceCaption: '主界面 - 简洁的作业编辑区',
    historyQueryCaption: '历史作业查询 - 快速检索过往记录',
    settingsInterface: '设置界面',
    settingsInterfaceCaption: '设置界面 - 个性化配置中心',
    languageSettings: '语言设置',
    languageSettingsCaption: '多语言支持 - 11种语言可选'
  },
  en: {
    features: 'Features',
    screenshots: 'Screenshots',
    download: 'Download',
    techStack: 'Tech Stack',
    subtitle: 'Classroom Homework Display Tool',
    autoSave: 'Auto Save',
    historyQuery: 'History Query',
    multiLang: '11 Languages',
    downloadNow: 'Download Now',
    viewDocs: 'View Docs',
    coreFeatures: 'Core Features',
    autoSaveTitle: 'Implicit Save',
    autoSaveDesc: 'Content changes are automatically saved to database with SHA256 integrity check to prevent data corruption',
    historyQueryTitle: 'History Query',
    historyQueryDesc: 'Built-in homework database supports quick date-based retrieval for easy backtracking',
    multiLangTitle: 'Multi-language Support',
    multiLangDesc: 'Supports 11 languages (Chinese, English, French, Spanish, Arabic, Russian, etc.)',
    lockMechanismTitle: 'Lock Mechanism',
    lockMechanismDesc: 'Default locked state prevents accidental touches, 60+ cute prompts, Ctrl+~ shortcut',
    screenshotTitle: 'One-click Screenshot',
    screenshotDesc: 'Screenshots automatically saved to clipboard for quick sharing to class groups',
    customSettingsTitle: 'Personalized Settings',
    customSettingsDesc: 'Customize fonts, font size (0-100px), initial templates for your unique experience',
    interfaceShowcase: 'Interface Showcase',
    githubReleasesDesc: 'Get the latest and historical versions here',
    giteeReleasesDesc: 'Recommended for users in China, faster download speed',
    goToGithub: 'Download from GitHub',
    goToGitee: 'Download from Gitee',
    systemRequirements: 'System Requirements',
    osRequirement: '✅ OS: Windows 10 (21H2+) / 11 (64-bit only)',
    no32bitNote: '❌ 32-bit systems, Windows 7 and older not supported',
    cpuRequirement: '🖥️ CPU: x86-64 Intel or AMD processor',
    diskRequirement: '💾 Disk: at least 500MB free space',
    macLinuxComing: '🔜 macOS / Linux coming soon',
    coreTech: 'Core Technology',
    devLang: 'Development Language',
    uiFramework: 'UI Framework',
    buildTool: 'Build Tool',
    keyDependencies: 'Key Dependencies',
    jsonParse: 'JSON Parsing',
    httpRequest: 'HTTP Requests',
    logging: 'Logging',
    markdownRender: 'Markdown Rendering',
    archPattern: 'Architecture Pattern',
    designPattern: 'Design Pattern',
    businessLogic: 'Business Logic Layer',
    dataStorage: 'Data Storage',
    githubRepo: 'GitHub Repository',
    issueFeedback: 'Issue Feedback',
    license: 'Open Source License: GNU GPL V3 🔓',
    mainInterface: 'Main Interface',
    mainInterfaceCaption: 'Main Interface - Simple Homework Editing Area',
    historyQueryCaption: 'History Homework Query - Quick Search Past Records',
    settingsInterface: 'Settings Interface',
    settingsInterfaceCaption: 'Settings Interface - Personalized Configuration Center',
    languageSettings: 'Language Settings',
    languageSettingsCaption: 'Multi-language Support - 11 Languages Available'
  }
};

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
let mainApp = null;

document.addEventListener('DOMContentLoaded', () => {
  const { createApp } = Vue;
  
  // 创建主应用实例
  mainApp = createApp({
    data() {
      return {
        currentLang: localStorage.getItem('language') || 'zh',
        screenshots: [
          {
            image: 'pic/en/20260406/mainpage.png',
            title: 'mainInterface',
            caption: 'mainInterfaceCaption'
          },
          {
            image: 'pic/en/historical-homework-search.png',
            title: 'historyQuery',
            caption: 'historyQueryCaption'
          },
          {
            image: 'pic/en/20260406/settings.png',
            title: 'settingsInterface',
            caption: 'settingsInterfaceCaption'
          },
          {
            image: 'pic/en/20260406/lang.png',
            title: 'languageSettings',
            caption: 'languageSettingsCaption'
          }
        ]
      };
    },
    computed: {
      // 使用计算属性确保响应式
      i18nData() {
        return i18n[this.currentLang];
      }
    },
    methods: {
      t(key) {
        return this.i18nData[key] || key;
      },
      handleLanguageChange(command) {
        this.currentLang = command;
        localStorage.setItem('language', command);
        document.documentElement.lang = command === 'zh' ? 'zh-CN' : 'en';
      }
    },
    mounted() {
      // 设置初始语言
      document.documentElement.lang = this.currentLang === 'zh' ? 'zh-CN' : 'en';
    }
  });
  
  mainApp.use(ElementPlus);
  mainApp.mount('#app');
});

// Anime.js动画
document.addEventListener('DOMContentLoaded', () => {
  // 初始化粒子背景
  new ParticleSystem();
  
  // 卡片鼠标跟随光照效果
  const cards = document.querySelectorAll('.glass-card');
  
  document.addEventListener('mousemove', (e) => {
    const mouseX = e.clientX;
    const mouseY = e.clientY;
    
    cards.forEach(card => {
      const rect = card.getBoundingClientRect();
      
      // 计算鼠标到卡片边界的最近距离
      const closestX = Math.max(rect.left, Math.min(mouseX, rect.right));
      const closestY = Math.max(rect.top, Math.min(mouseY, rect.bottom));
      
      const distanceX = mouseX - closestX;
      const distanceY = mouseY - closestY;
      const distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
      
      // 触发距离：300px
      const triggerDistance = 300;
      
      if (distance < triggerDistance) {
        // 计算光照强度（距离越近，光照越强）
        const intensity = 1 - (distance / triggerDistance);
        
        // 计算光照位置（相对于卡片，限制在 0-100% 范围内）
        const x = Math.max(0, Math.min(100, ((mouseX - rect.left) / rect.width) * 100));
        const y = Math.max(0, Math.min(100, ((mouseY - rect.top) / rect.height) * 100));
        
        // 检查是否为 Gitee 卡片
        const isGiteeCard = card.classList.contains('gitee-card');
        const glowColor = isGiteeCard ? `rgba(230, 70, 26, ${intensity * 0.15})` : `rgba(90, 156, 240, ${intensity * 0.15})`;
        const glowShadow = isGiteeCard ? `rgba(230, 70, 26, ${intensity * 0.3})` : `rgba(90, 156, 240, ${intensity * 0.3})`;
        
        // 应用光照效果
        card.style.background = `radial-gradient(circle at ${x}% ${y}%, ${glowColor}, rgba(30, 30, 30, 0.15))`;
        card.style.boxShadow = `
          0 8px 32px rgba(0, 0, 0, 0.15),
          0 0 ${intensity * 30}px ${glowShadow},
          0 0 0 1px rgba(255, 255, 255, 0.02) inset,
          0 1px 0 rgba(255, 255, 255, 0.06) inset
        `;
      } else {
        // 恢复默认样式
        card.style.background = 'rgba(30, 30, 30, 0.15)';
        card.style.boxShadow = `
          0 8px 32px rgba(0, 0, 0, 0.15),
          0 0 0 1px rgba(255, 255, 255, 0.02) inset,
          0 1px 0 rgba(255, 255, 255, 0.06) inset
        `;
      }
    });
  });
  
  // 语言切换按钮点击波纹效果
  const langSwitcher = document.querySelector('.language-switcher .el-dropdown-link');
  if (langSwitcher) {
    langSwitcher.addEventListener('click', function(e) {
      const rect = this.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      
      const ripple = document.createElement('span');
      ripple.classList.add('ripple');
      ripple.style.left = x + 'px';
      ripple.style.top = y + 'px';
      
      this.appendChild(ripple);
      
      setTimeout(() => ripple.remove(), 600);
    });
  }
  
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
