<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <meta name="description" content="Admin pages">
    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>系统管理</title>
    <script src="/static/css/tailwind.css"></script>
    <!-- <link rel="stylesheet" href="/static/css/tw_out.css"/> -->
    <link rel="stylesheet" href="/static/css/style_admin.css"/>
</head>
<body class="bg-gray-100 flex flex-col min-h-screen">
    <div class="flex flex-1">
        <aside class="w-64 bg-gray-800 text-white
                      p-5 min-h-screen">
            <h2 class="text-2xl font-bold mb-5">
                <a href="/admin">系统管理</a>
            </h2>
            {{menu|safe}}
        </aside>

        <main class="flex-1 p-6">
            <h1 class="text-2xl font-bold mb-6">
                {{title}}
            </h1>
            {{flash|safe}}
            {{content|safe}}
        </main>
    </div>

    <div id="div-modal"></div>

    <footer class="bg-gray-800 text-white text-center
                   p-3 mt-auto">
       &copy; 2025 网管2.0系统管理 绍兴电信云中台
    </footer>
    <script>
        function hideFlashMessage() {
          const flash = document.getElementById('flashMessage');
          flash.style.opacity = '0';
          setTimeout(() => flash.classList.add('hidden'), 300);
        }
    </script>
    <script src="/static/js/htmx.min.js"></script>
    <script src="/static/js/hyperscript.min.js"></script>
</body>
</html>