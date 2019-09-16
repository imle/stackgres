var Nav = Vue.component("sg-nav", {
	template: `
	<aside id="nav" class="disabled">

			<a href="#" id="collapse" class="nav-item">
				<svg xmlns="http://www.w3.org/2000/svg" width="43.657" height="37.19" viewBox="0 0 43.657 37.19"><path d="M43.258 20.444l-2.371-2.724 1.412-.819a.1.1 0 0 0 .049-.1v-4.6c.025-.049 0-.074-.049-.1a.075.075 0 0 0-.1 0l-6.047-3.487-6.4-7.36a.115.115 0 0 0-.148-.025l-3.108 1.814-4.593-2.65a.112.112 0 0 0-.123 0L1.359 12.129a.024.024 0 0 0-.025.025l-.025.025v4.65a.128.128 0 0 0 .049.1l1.394.806L.4 20.444c0 .025-.025.049-.025.074v4.379a.091.091 0 0 0 .025.074l9.644 11.588.025.025h.049l23.373.049h.074a.024.024 0 0 0 .025-.025l9.644-11.047a.091.091 0 0 0 .025-.074l.025-4.97a.09.09 0 0 0-.026-.073zm-.246.074l-2.591 2.966a4.038 4.038 0 0 0 .279-.6v-4.088c.049-.025.049-.049.049-.049a3.117 3.117 0 0 0-.129-.871l.059-.034zM3.155 22.806v-2.854c1.2 2.976 6.783 5.376 14.006 6.112l4.618 2.671c.025 0 .049.025.074.025s.049 0 .049-.025l4.606-2.672c7.211-.737 12.772-3.132 13.994-6.086v2.829c-1.279 3.518-9.078 7.278-18.673 7.278s-17.37-3.76-18.674-7.278zm13.488 2.959c-7.774-.881-13.488-3.689-13.488-7.018a2.94 2.94 0 0 1 .095-.727zM6.083 15.278l31.467-4.576v3.912h.025l-31.492 4.28zm31.192-.336l-15.422 8.942-9.61-5.549zm3.127 3.06a2.936 2.936 0 0 1 .1.745c0 3.329-5.7 6.138-13.481 7.018zm1.723-1.3L21.976 28.416v-4.332l20.15-11.686zm-.172-4.478l-4.157 2.412.024-4.085a.02.02 0 0 0-.006-.016.15.15 0 0 0-.019-.034l-1.187-1.363zM29.628 1.475l7.824 9-13.581 1.973-17.3 2.509zM21.853.614l4.424 2.556-20.045 11.7-4.233-2.446-.344-.2zM1.531 12.448l4.445 2.57-.066.038-.025.025-.025.025a.024.024 0 0 1-.025.025v3.936a.185.185 0 0 0 .049.1.09.09 0 0 0 .074.025h.025l5.838-.793 9.887 5.711v4.33L1.531 16.779zm1.43 5.4l.071.041a3.112 3.112 0 0 0-.123.853v4.142a4.035 4.035 0 0 0 .2.442l-.172-.2-2.288-2.608zm7.058 18.264l-9.4-11.293v-3.981l5.339 6.077 4.06 4.625zm23.4.3l-23.127-.049v-4.724l11.662.025 11.465.025zm.049-4.97l-11.59-.025-11.686-.025-5.074-5.778c3.245 2.578 9.466 4.721 16.686 4.721 7.377 0 13.724-2.237 16.9-4.89zm9.57-6.028l-9.4 10.776v-4.6l9.4-10.776z" stroke-linecap="round" stroke-width=".75"/></svg>
			</a>
			
			<router-link :to="'/overview/'" title="Overview" class="view nav-item">
				<svg xmlns="http://www.w3.org/2000/svg" width="28" height="16.333" viewBox="0 0 28 16.333"><path data-name="Trazado 2234" d="M17.5 8.167a3.5 3.5 0 1 1-6.934-.678 2.181 2.181 0 0 0 2.806-2.766A3.694 3.694 0 0 1 14 4.667a3.5 3.5 0 0 1 3.5 3.5zM14.017 0C5.187 0 0 7.643 0 7.643s5.641 8.69 14.017 8.69c9.022 0 13.983-8.69 13.983-8.69S22.994 0 14.017 0zM14 14a5.833 5.833 0 1 1 5.833-5.833A5.833 5.833 0 0 1 14 14z"/></svg>
			</router-link>

			<div class="top">
				<router-link :to="( (this.$route.params.name == null) || (currentCluster == '') ) ? '/' : '/information/'+this.$route.params.name" title="Information" class="info nav-item">
					<svg xmlns="http://www.w3.org/2000/svg" width="22" height="20.167" viewBox="0 0 22 20.167"><path data-name="Trazado 2236" d="M11 0C5.19 0 0 3.874 0 9.173a8.082 8.082 0 0 0 1.876 5.156c.05 1.678-.938 4.085-1.827 5.837a26.4 26.4 0 0 0 7.313-2.325C15.829 19.9 22 14.721 22 9.173 22 3.845 16.774 0 11 0zm.917 13.75h-1.834v-5.5h1.833zM11 6.646A1.146 1.146 0 1 1 12.146 5.5 1.146 1.146 0 0 1 11 6.646z"/></svg>
				</router-link>

				<router-link :to="( (this.$route.params.name == null) || (currentCluster == '') ) ? '/' : '/status/'+this.$route.params.name" title="Status" class="status nav-item">
					<svg xmlns="http://www.w3.org/2000/svg" width="24" height="20" viewBox="0 0 24 20"><path data-name="Trazado 2238" d="M18.905 12c-2.029 2.4-4.862 5-7.905 8C5.107 14.2 0 9.866 0 5.629 0-.525 8.114-1.958 11 2.953c2.865-4.875 11-3.5 11 2.676A6.323 6.323 0 0 1 21.5 8h-6.275a.7.7 0 0 0-.61.358l-.813 1.45-2.27-4.437a.7.7 0 0 0-1.2-.081L8.454 8H7.227a2 2 0 1 0 0 2h1.956a.7.7 0 0 0 .573-.3l.989-1.406L13 12.856a.7.7 0 0 0 1.227.052L15.987 10H24v2z" fill-rule="evenodd"/></svg>
				</router-link>

				<div class="tooltip">Please select a cluster.</div>
			</div>

			<footer class="bottom">
				<div class="help">
					<a href="#" title="Need some help?" class="nav-item">
						<svg xmlns="http://www.w3.org/2000/svg" width="27.378" height="27.378" viewBox="0 0 27.378 27.378"><path d="M26.919 10.17a21.434 21.434 0 0 1-.475-3.845c-.191-3.366-1.8-5.446-5.578-5.508A17.147 17.147 0 0 1 16.85.365 13.86 13.86 0 0 0 13.69 0a13.715 13.715 0 0 0-3.613.483 17.493 17.493 0 0 1-3.488.335C2.813.878 1.2 2.958 1.011 6.324A23.44 23.44 0 0 1 .7 9.363a13.733 13.733 0 0 0 0 8.653 23.405 23.405 0 0 1 .311 3.038C1.2 24.42 2.813 26.5 6.589 26.56a17.467 17.467 0 0 1 3.492.337 13.773 13.773 0 0 0 6.769.114 17.135 17.135 0 0 1 4.017-.451c3.777-.06 5.389-2.14 5.578-5.506a21.455 21.455 0 0 1 .472-3.833 13.748 13.748 0 0 0 0-7.051zm-1.613-3.78l.007.121a13.713 13.713 0 0 0-4.623-4.557h.159c2.912.051 4.287 1.418 4.457 4.435zm-4.354 3.995l2.975-1.695a11.272 11.272 0 0 1 0 10l-2.975-1.694a7.926 7.926 0 0 0 0-6.611zM13.689 25.1a11.325 11.325 0 0 1-5.016-1.177l1.694-2.975a7.922 7.922 0 0 0 6.644 0l1.689 2.971a11.325 11.325 0 0 1-5.011 1.181zm0-5.7a5.7 5.7 0 1 1 5.7-5.7 5.71 5.71 0 0 1-5.7 5.693zm0-17.111a11.325 11.325 0 0 1 5.011 1.17l-1.689 2.975a7.922 7.922 0 0 0-6.644 0L8.673 3.459a11.325 11.325 0 0 1 5.016-1.178zm-7.082-.33h.075a13.724 13.724 0 0 0-4.53 4.4c.179-2.995 1.553-4.354 4.455-4.4zm-.181 15.035l-2.975 1.694a11.272 11.272 0 0 1 0-10l2.975 1.694a7.926 7.926 0 0 0 0 6.611zm-4.271 4.033A13.709 13.709 0 0 0 6.7 25.422h-.089c-2.902-.05-4.277-1.406-4.456-4.395zm18.693 4.392h-.171a13.706 13.706 0 0 0 4.636-4.548l-.007.115c-.17 3.02-1.545 4.386-4.458 4.433z"/></svg>
					</a>

					<div class="tooltip">
						<p>NEED SOME HELP?</p>
						<p>
							Contact us at<br/>
							<a href="mailto:mail@stackgres.io">mail@stackgres.io</a>
						</p>
					</div>
				</div>

				<div id="notifications" class="active">
					<a href="#" title="Notifications" class="nav-item">
						<span class="loader"></span>
						<span class="count">5</span>
						<svg xmlns="http://www.w3.org/2000/svg" width="20" height="24" viewBox="0 0 20 24"><path data-name="Trazado 2239" d="M13.137 3.945a2.1 2.1 0 0 1-1.037-1.82 2.1 2.1 0 1 0-4.193 0 2.1 2.1 0 0 1-1.041 1.82C2.195 6.657 4.877 15.66 0 17.251V19h20v-1.749c-4.877-1.591-2.195-10.594-6.863-13.306zM13 21a3.066 3.066 0 0 1-2.971 3A3.118 3.118 0 0 1 7 21z"/></svg>
					</a>

					<div class="tooltip">
						<p>NOTIFICATIONS</p>
						<hr/>
						<p>Changes have been saved succesfully.</p>
					</div>
				</div>
			</footer>
		</aside>`,
})