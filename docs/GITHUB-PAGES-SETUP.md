# GitHub Pages Setup Instructions

This document explains how to enable and configure GitHub Pages for the DeathBan Pro landing page.

## Automatic Setup (Recommended)

Once this PR is merged to the `main` branch, the GitHub Pages deployment workflow will automatically run. However, you need to enable GitHub Pages in the repository settings first.

## Manual Configuration Steps

### 1. Enable GitHub Pages

1. Go to the repository on GitHub: `https://github.com/KevinTCoughlin/deathban-pro`
2. Click on **Settings** (gear icon)
3. In the left sidebar, click on **Pages**
4. Under **Build and deployment**:
   - **Source**: Select "GitHub Actions"
   - This will allow the workflow in `.github/workflows/pages.yml` to deploy the site

### 2. Verify Workflow Permissions

1. In the repository settings, go to **Actions** → **General**
2. Scroll down to **Workflow permissions**
3. Ensure that either:
   - "Read and write permissions" is selected, OR
   - "Read repository contents and packages permissions" is selected with "Allow GitHub Actions to create and approve pull requests" enabled

### 3. Custom Domain Setup (Optional)

Once GitHub Pages is enabled and the site is deployed:

1. Go back to **Settings** → **Pages**
2. Under **Custom domain**, enter your custom domain (e.g., `deathban-pro.example.com`)
3. Click **Save**
4. GitHub will create a `CNAME` file in the repository
5. Configure your DNS provider:
   - Add a CNAME record pointing to `kevintcoughlin.github.io`
   - Or for apex domain, add A records pointing to GitHub Pages IPs:
     - `185.199.108.153`
     - `185.199.109.153`
     - `185.199.110.153`
     - `185.199.111.153`

### 4. Enable HTTPS

1. After the domain is configured, GitHub will automatically provision an SSL certificate
2. Check the **Enforce HTTPS** box (it will be available after DNS propagates)

## Deployment

The landing page will be automatically deployed when:
- Code is pushed to the `main` branch
- The workflow is manually triggered from the Actions tab

## Files Included

- `index.html` - The landing page for DeathBan Pro
- `.github/workflows/pages.yml` - GitHub Actions workflow for deployment
- `.nojekyll` - Tells GitHub Pages to skip Jekyll processing

## Testing Locally

To test the landing page locally before deployment:

```bash
# Using Python's built-in HTTP server
python3 -m http.server 8000

# Or using Node.js http-server
npx http-server

# Then open http://localhost:8000 in your browser
```

## Site URL

Once deployed, the site will be available at:
- Default: `https://kevintcoughlin.github.io/deathban-pro/`
- Custom domain: `https://your-custom-domain.com` (after DNS configuration)

## Troubleshooting

### Workflow fails with permission error
- Check the workflow permissions in Settings → Actions → General
- Ensure "pages: write" and "id-token: write" permissions are allowed

### Site shows 404
- Ensure GitHub Pages is enabled in Settings → Pages
- Check that the workflow completed successfully in the Actions tab
- Wait a few minutes for DNS propagation

### Custom domain not working
- Verify DNS records with `dig your-domain.com` or `nslookup your-domain.com`
- DNS propagation can take up to 24-48 hours
- Ensure the CNAME file was created in the repository root

## Maintenance

- The landing page content is in `index.html` - edit this file to update the site
- The workflow will automatically redeploy on every push to `main`
- Monitor deployments in the Actions tab
